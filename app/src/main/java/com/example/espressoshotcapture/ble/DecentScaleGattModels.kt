package com.example.espressoshotcapture.ble

import kotlinx.coroutines.flow.StateFlow

sealed class DecentScaleGattConnectionState {
    data object Disconnected : DecentScaleGattConnectionState()
    data object Connecting : DecentScaleGattConnectionState()
    data object Connected : DecentScaleGattConnectionState()
    data object ServiceDiscoveryFailed : DecentScaleGattConnectionState()
    data object NotificationSetupFailed : DecentScaleGattConnectionState()
    data object ReceivingReadings : DecentScaleGattConnectionState()
    data class Error(val message: String) : DecentScaleGattConnectionState()
}

sealed class DecentScaleTareStatus {
    data object Idle : DecentScaleTareStatus()
    data object Sending : DecentScaleTareStatus()
    data object Sent : DecentScaleTareStatus()
    data class Failed(val message: String) : DecentScaleTareStatus()
}

data class DecentScaleGattState(
    val connectionState: DecentScaleGattConnectionState = DecentScaleGattConnectionState.Disconnected,
    val notifyCharacteristicFound: Boolean? = null,
    val writeCharacteristicFound: Boolean? = null,
    val latestRawPacketHex: String? = null,
    val latestWeightGrams: Double? = null,
    val latestReadingTimestampMs: Long? = null,
    val latestParserError: String? = null,
    val tareStatus: DecentScaleTareStatus = DecentScaleTareStatus.Idle
) {
    val connectionLabel: String
        get() = when (val state = connectionState) {
            DecentScaleGattConnectionState.Disconnected -> "GATT: disconnected"
            DecentScaleGattConnectionState.Connecting -> "GATT: connecting"
            DecentScaleGattConnectionState.Connected -> "GATT: connected"
            DecentScaleGattConnectionState.ServiceDiscoveryFailed -> "GATT: service discovery failed"
            DecentScaleGattConnectionState.NotificationSetupFailed -> "GATT: notification setup failed"
            DecentScaleGattConnectionState.ReceivingReadings -> "GATT: receiving readings"
            is DecentScaleGattConnectionState.Error -> "GATT: error - ${state.message}"
        }

    val latestWeightLabel: String
        get() = latestWeightGrams?.let { weightGrams ->
            "Latest weight: $weightGrams g"
        } ?: "Latest weight: none"

    val latestRawPacketLabel: String
        get() = latestRawPacketHex?.let { rawPacket ->
            "Latest raw packet: $rawPacket"
        } ?: "Latest raw packet: none"

    val latestTimestampLabel: String
        get() = latestReadingTimestampMs?.let { timestampMs ->
            "Latest reading timestamp: $timestampMs"
        } ?: "Latest reading timestamp: none"

    val notifyCharacteristicLabel: String
        get() = "Notify characteristic fff4: ${notifyCharacteristicFound.toDiscoveryLabel()}"

    val writeCharacteristicLabel: String
        get() = "Write characteristic 36f5: ${writeCharacteristicFound.toDiscoveryLabel()}"

    val tareStatusLabel: String
        get() = when (val status = tareStatus) {
            DecentScaleTareStatus.Idle -> "Tare: idle"
            DecentScaleTareStatus.Sending -> "Tare: sending"
            DecentScaleTareStatus.Sent -> "Tare: sent"
            is DecentScaleTareStatus.Failed -> "Tare: failed - ${status.message}"
        }

    val canSendTare: Boolean
        get() = writeCharacteristicFound == true &&
            connectionState.isWritableConnection() &&
            tareStatus != DecentScaleTareStatus.Sending

    private fun Boolean?.toDiscoveryLabel(): String =
        when (this) {
            true -> "found"
            false -> "missing"
            null -> "unknown"
        }

    private fun DecentScaleGattConnectionState.isWritableConnection(): Boolean =
        this == DecentScaleGattConnectionState.Connected ||
            this == DecentScaleGattConnectionState.ReceivingReadings
}

interface DecentScaleGattClient {
    val state: StateFlow<DecentScaleGattState>

    fun connect(candidate: BleScaleScanCandidate)
    fun disconnect()
    fun sendTare()
}
