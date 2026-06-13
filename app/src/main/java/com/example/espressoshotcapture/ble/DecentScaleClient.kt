package com.example.espressoshotcapture.ble

import com.example.espressoshotcapture.capture.domain.ScaleClient
import com.example.espressoshotcapture.capture.domain.ScaleConnectionState
import com.example.espressoshotcapture.capture.domain.ScaleReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class DecentScaleClient(
    private val gattClient: DecentScaleGattClient,
    private val candidate: BleScaleScanCandidate
) : ScaleClient {
    override val connectionState: Flow<ScaleConnectionState> =
        gattClient.state
            .map { state ->
                DecentScaleClientMapper.toScaleConnectionState(state.connectionState)
            }
            .distinctUntilChanged()

    override val readings: Flow<ScaleReading> =
        gattClient.state
            .mapNotNull(DecentScaleClientMapper::toScaleReadingOrNull)
            .distinctUntilChanged()

    override fun connect() {
        gattClient.connect(candidate)
    }

    override fun disconnect() {
        gattClient.disconnect()
    }
}

object DecentScaleClientMapper {
    fun toScaleConnectionState(
        gattState: DecentScaleGattConnectionState
    ): ScaleConnectionState =
        when (gattState) {
            DecentScaleGattConnectionState.Disconnected -> ScaleConnectionState.Disconnected
            DecentScaleGattConnectionState.Connecting -> ScaleConnectionState.Connecting
            DecentScaleGattConnectionState.Connected -> ScaleConnectionState.Connected
            DecentScaleGattConnectionState.ReceivingReadings -> ScaleConnectionState.Connected
            DecentScaleGattConnectionState.ServiceDiscoveryFailed ->
                ScaleConnectionState.Error("Service discovery failed")
            DecentScaleGattConnectionState.NotificationSetupFailed ->
                ScaleConnectionState.Error("Notification setup failed")
            is DecentScaleGattConnectionState.Error -> ScaleConnectionState.Error(gattState.message)
        }

    fun toScaleReadingOrNull(gattState: DecentScaleGattState): ScaleReading? {
        val timestampMs = gattState.latestReadingTimestampMs ?: return null
        val weightGrams = gattState.latestWeightGrams ?: return null
        return ScaleReading(
            timestampMillis = timestampMs,
            weightGrams = weightGrams
        )
    }
}
