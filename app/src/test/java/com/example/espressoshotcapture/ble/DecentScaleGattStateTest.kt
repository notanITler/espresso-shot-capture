package com.example.espressoshotcapture.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecentScaleGattStateTest {
    @Test
    fun tareIsAvailableOnlyWhenWritableConnectionExists() {
        assertFalse(
            DecentScaleGattState(
                connectionState = DecentScaleGattConnectionState.Disconnected,
                writeCharacteristicFound = true
            ).canSendTare
        )
        assertFalse(
            DecentScaleGattState(
                connectionState = DecentScaleGattConnectionState.ReceivingReadings,
                writeCharacteristicFound = false
            ).canSendTare
        )
        assertTrue(
            DecentScaleGattState(
                connectionState = DecentScaleGattConnectionState.ReceivingReadings,
                writeCharacteristicFound = true
            ).canSendTare
        )
    }

    @Test
    fun tareStatusLabelsAreHumanReadable() {
        assertEquals(
            "Tare: idle",
            DecentScaleGattState().tareStatusLabel
        )
        assertEquals(
            "Tare: sending",
            DecentScaleGattState(tareStatus = DecentScaleTareStatus.Sending).tareStatusLabel
        )
        assertEquals(
            "Tare: sent",
            DecentScaleGattState(tareStatus = DecentScaleTareStatus.Sent).tareStatusLabel
        )
        assertEquals(
            "Tare: failed - Write failed",
            DecentScaleGattState(
                tareStatus = DecentScaleTareStatus.Failed("Write failed")
            ).tareStatusLabel
        )
    }
}
