package com.example.espressoshotcapture.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@Suppress("DEPRECATION")
class AndroidDecentScaleGattClient(
    context: Context
) : DecentScaleGattClient {
    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val _state = MutableStateFlow(DecentScaleGattState())
    override val state: StateFlow<DecentScaleGattState> = _state.asStateFlow()
    private var bluetoothGatt: BluetoothGatt? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                closeGatt(gatt)
                updateConnectionState(
                    DecentScaleGattConnectionState.Error("Connection failed: $status")
                )
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    updateConnectionState(DecentScaleGattConnectionState.Connected)
                    if (!hasConnectPermission()) {
                        updateConnectionState(
                            DecentScaleGattConnectionState.Error("Bluetooth connect permission missing")
                        )
                        return
                    }
                    if (!gatt.discoverServices()) {
                        updateConnectionState(DecentScaleGattConnectionState.ServiceDiscoveryFailed)
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    closeGatt(gatt)
                    _state.value = DecentScaleGattState()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                updateConnectionState(DecentScaleGattConnectionState.ServiceDiscoveryFailed)
                return
            }

            val service = gatt.getService(DECENT_SCALE_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(DECENT_SCALE_NOTIFY_UUID)
            val writeCharacteristic = service?.getCharacteristic(DECENT_SCALE_WRITE_UUID)
            _state.value = _state.value.copy(
                notifyCharacteristicFound = characteristic != null,
                writeCharacteristicFound = writeCharacteristic != null
            )
            if (service == null || characteristic == null) {
                updateConnectionState(DecentScaleGattConnectionState.ServiceDiscoveryFailed)
                return
            }

            notifyCharacteristic = characteristic
            enableWeightNotifications(gatt, characteristic)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid != CLIENT_CHARACTERISTIC_CONFIG_UUID) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateConnectionState(DecentScaleGattConnectionState.ReceivingReadings)
            } else {
                updateConnectionState(DecentScaleGattConnectionState.NotificationSetupFailed)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == DECENT_SCALE_NOTIFY_UUID) {
                handleNotification(characteristic.value ?: return)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == DECENT_SCALE_NOTIFY_UUID) {
                handleNotification(value)
            }
        }
    }

    override fun connect(candidate: BleScaleScanCandidate) {
        val address = candidate.address
        if (address.isNullOrBlank()) {
            updateConnectionState(DecentScaleGattConnectionState.Error("Device address missing"))
            return
        }

        if (!hasConnectPermission()) {
            updateConnectionState(
                DecentScaleGattConnectionState.Error("Bluetooth connect permission missing")
            )
            return
        }

        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            updateConnectionState(DecentScaleGattConnectionState.Error("Bluetooth unavailable"))
            return
        }

        disconnect()
        _state.value = DecentScaleGattState(
            connectionState = DecentScaleGattConnectionState.Connecting
        )

        try {
            val device = adapter.getRemoteDevice(address)
            bluetoothGatt = device.connectGatt(
                appContext,
                false,
                callback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (exception: IllegalArgumentException) {
            updateConnectionState(DecentScaleGattConnectionState.Error("Invalid device address"))
        } catch (exception: SecurityException) {
            updateConnectionState(
                DecentScaleGattConnectionState.Error("Bluetooth connect permission missing")
            )
        }
    }

    override fun disconnect() {
        val gatt = bluetoothGatt ?: return
        bluetoothGatt = null
        notifyCharacteristic = null
        try {
            if (hasConnectPermission()) {
                gatt.disconnect()
            }
        } catch (exception: SecurityException) {
            // Close below still releases local resources for this spike.
        } finally {
            closeGatt(gatt)
            _state.value = DecentScaleGattState()
        }
    }

    private fun enableWeightNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        if (!hasConnectPermission()) {
            updateConnectionState(
                DecentScaleGattConnectionState.Error("Bluetooth connect permission missing")
            )
            return
        }

        try {
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                updateConnectionState(DecentScaleGattConnectionState.NotificationSetupFailed)
                return
            }

            val subscriptionValue = when {
                (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ->
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 ->
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                else -> {
                    updateConnectionState(DecentScaleGattConnectionState.NotificationSetupFailed)
                    return
                }
            }

            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (descriptor == null) {
                updateConnectionState(DecentScaleGattConnectionState.NotificationSetupFailed)
                return
            }

            val writeStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(
                    descriptor,
                    subscriptionValue
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                descriptor.value = subscriptionValue
                gatt.writeDescriptor(descriptor)
            }

            if (!writeStarted) {
                updateConnectionState(DecentScaleGattConnectionState.NotificationSetupFailed)
            }
        } catch (exception: SecurityException) {
            updateConnectionState(
                DecentScaleGattConnectionState.Error("Bluetooth connect permission missing")
            )
        }
    }

    private fun handleNotification(packet: ByteArray) {
        val rawHex = DecentScaleWeightPacketParser.toHex(packet)
        when (val result = DecentScaleWeightPacketParser.parse(packet)) {
            is DecentScaleWeightPacketParseResult.Success -> {
                _state.value = _state.value.copy(
                    connectionState = DecentScaleGattConnectionState.ReceivingReadings,
                    latestRawPacketHex = rawHex,
                    latestWeightGrams = result.reading.weightGrams,
                    latestReadingTimestampMs = System.currentTimeMillis(),
                    latestParserError = null
                )
            }

            is DecentScaleWeightPacketParseResult.Invalid -> {
                _state.value = _state.value.copy(
                    latestRawPacketHex = rawHex,
                    latestParserError = result.reason
                )
            }
        }
    }

    private fun updateConnectionState(connectionState: DecentScaleGattConnectionState) {
        _state.value = _state.value.copy(connectionState = connectionState)
    }

    private fun closeGatt(gatt: BluetoothGatt) {
        if (bluetoothGatt === gatt) {
            bluetoothGatt = null
        }
        notifyCharacteristic = null
        gatt.close()
    }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private val DECENT_SCALE_SERVICE_UUID: UUID =
            UUID.fromString(HalfDecentScaleMatcher.SERVICE_UUID_FULL)
        private val DECENT_SCALE_NOTIFY_UUID: UUID =
            UUID.fromString(HalfDecentScaleMatcher.READ_NOTIFY_CHARACTERISTIC_UUID_FULL)
        private val DECENT_SCALE_WRITE_UUID: UUID =
            UUID.fromString(HalfDecentScaleMatcher.WRITE_CHARACTERISTIC_UUID_FULL)
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
