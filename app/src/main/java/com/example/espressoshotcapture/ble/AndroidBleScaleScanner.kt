package com.example.espressoshotcapture.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidBleScaleScanner(
    context: Context
) : BleScaleScanner {
    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val _scanState = MutableStateFlow(BleScaleScanState())
    override val scanState: StateFlow<BleScaleScanState> = _scanState.asStateFlow()
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::addScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _scanState.value = _scanState.value.copy(
                status = BleScaleScanStatus.ERROR,
                errorMessage = "Scan failed: $errorCode"
            )
        }
    }

    override fun startScan() {
        if (!hasRequiredPermissions()) {
            markPermissionRequired()
            return
        }

        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _scanState.value = _scanState.value.copy(
                status = BleScaleScanStatus.BLUETOOTH_UNAVAILABLE,
                errorMessage = null
            )
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _scanState.value = _scanState.value.copy(
                status = BleScaleScanStatus.BLUETOOTH_UNAVAILABLE,
                errorMessage = null
            )
            return
        }

        if (isScanning) return

        _scanState.value = BleScaleScanState(status = BleScaleScanStatus.SCANNING)
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            scanner.startScan(null, settings, scanCallback)
            isScanning = true
        } catch (exception: SecurityException) {
            markPermissionRequired()
        }
    }

    override fun stopScan() {
        if (!isScanning) return
        if (!hasRequiredPermissions()) {
            isScanning = false
            markPermissionRequired()
            return
        }

        try {
            bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (exception: SecurityException) {
            markPermissionRequired()
        } finally {
            isScanning = false
            if (_scanState.value.status == BleScaleScanStatus.SCANNING) {
                _scanState.value = _scanState.value.copy(status = BleScaleScanStatus.IDLE)
            }
        }
    }

    override fun markPermissionRequired() {
        _scanState.value = _scanState.value.copy(
            status = BleScaleScanStatus.PERMISSION_REQUIRED,
            errorMessage = null
        )
    }

    private fun addScanResult(result: ScanResult) {
        val serviceUuids = result.scanRecord?.serviceUuids
            ?.map { parcelUuid -> parcelUuid.uuid.toString() }
            ?: emptyList()
        val name = result.scanRecord?.deviceName
            ?: runCatching { result.device.name }.getOrNull()
        val address = runCatching { result.device.address }.getOrNull()

        val candidate = HalfDecentScaleMatcher.toCandidate(
            name = name,
            address = address,
            advertisedServiceUuids = serviceUuids,
            rssi = result.rssi
        )
        _scanState.value = _scanState.value.copy(
            status = BleScaleScanStatus.SCANNING,
            candidates = BleScaleScanCandidateList.upsert(
                existing = _scanState.value.candidates,
                candidate = candidate
            ),
            errorMessage = null
        )
    }

    private fun hasRequiredPermissions(): Boolean =
        requiredRuntimePermissions().all { permission ->
            ContextCompat.checkSelfPermission(appContext, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

    companion object {
        fun requiredRuntimePermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }
}
