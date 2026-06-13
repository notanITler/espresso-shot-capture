package com.example.espressoshotcapture.ble

import com.example.espressoshotcapture.capture.domain.ScaleConnectionState
import com.example.espressoshotcapture.capture.domain.ScaleReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecentScaleClientMapperTest {
    @Test
    fun mapsDisconnectedState() {
        assertEquals(
            ScaleConnectionState.Disconnected,
            DecentScaleClientMapper.toScaleConnectionState(
                DecentScaleGattConnectionState.Disconnected
            )
        )
    }

    @Test
    fun mapsConnectingState() {
        assertEquals(
            ScaleConnectionState.Connecting,
            DecentScaleClientMapper.toScaleConnectionState(
                DecentScaleGattConnectionState.Connecting
            )
        )
    }

    @Test
    fun mapsConnectedAndReceivingReadingsToConnected() {
        assertEquals(
            ScaleConnectionState.Connected,
            DecentScaleClientMapper.toScaleConnectionState(
                DecentScaleGattConnectionState.Connected
            )
        )
        assertEquals(
            ScaleConnectionState.Connected,
            DecentScaleClientMapper.toScaleConnectionState(
                DecentScaleGattConnectionState.ReceivingReadings
            )
        )
    }

    @Test
    fun mapsServiceDiscoveryFailureToError() {
        assertEquals(
            ScaleConnectionState.Error("Service discovery failed"),
            DecentScaleClientMapper.toScaleConnectionState(
                DecentScaleGattConnectionState.ServiceDiscoveryFailed
            )
        )
    }

    @Test
    fun mapsNotificationSetupFailureToError() {
        assertEquals(
            ScaleConnectionState.Error("Notification setup failed"),
            DecentScaleClientMapper.toScaleConnectionState(
                DecentScaleGattConnectionState.NotificationSetupFailed
            )
        )
    }

    @Test
    fun mapsGattErrorMessageToScaleError() {
        assertEquals(
            ScaleConnectionState.Error("Connection failed"),
            DecentScaleClientMapper.toScaleConnectionState(
                DecentScaleGattConnectionState.Error("Connection failed")
            )
        )
    }

    @Test
    fun mapsParsedGattReadingToScaleReading() {
        val reading = DecentScaleClientMapper.toScaleReadingOrNull(
            DecentScaleGattState(
                connectionState = DecentScaleGattConnectionState.ReceivingReadings,
                latestWeightGrams = 18.4,
                latestReadingTimestampMs = 12_345L
            )
        )

        assertEquals(
            ScaleReading(timestampMillis = 12_345L, weightGrams = 18.4),
            reading
        )
    }

    @Test
    fun ignoresGattStateWithoutCompleteReading() {
        assertNull(
            DecentScaleClientMapper.toScaleReadingOrNull(
                DecentScaleGattState(latestWeightGrams = 18.4)
            )
        )
        assertNull(
            DecentScaleClientMapper.toScaleReadingOrNull(
                DecentScaleGattState(latestReadingTimestampMs = 12_345L)
            )
        )
    }
}
