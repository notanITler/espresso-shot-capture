package com.example.espressoshotcapture.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ScaleConnectionUiStateTest {
    @Test
    fun connectedStatusHasPriorityOverScanningInHeader() {
        val label = bluetoothHeaderButtonLabel(
            uiState = BleScaleScanState(status = BleScaleScanStatus.SCANNING),
            gattState = DecentScaleGattState(
                connectionState = DecentScaleGattConnectionState.Connected
            )
        )

        assertEquals("Bluetooth: connected", label)
    }

    @Test
    fun receivingReadingsStatusHasPriorityOverScanningInHeader() {
        val label = bluetoothHeaderButtonLabel(
            uiState = BleScaleScanState(status = BleScaleScanStatus.SCANNING),
            gattState = DecentScaleGattState(
                connectionState = DecentScaleGattConnectionState.ReceivingReadings
            )
        )

        assertEquals("Bluetooth: receiving", label)
    }

    @Test
    fun candidateRowShowsConnectedInsteadOfTapToConnectWhenConnected() {
        val gattState = DecentScaleGattState(
            connectionState = DecentScaleGattConnectionState.Connected
        )

        assertEquals("Connected", decentScaleCandidateActionLabel(gattState))
        assertFalse(isDecentScaleCandidateConnectEnabled(gattState))
    }

    @Test
    fun candidateRowShowsReceivingReadingsWhenReceivingReadings() {
        val gattState = DecentScaleGattState(
            connectionState = DecentScaleGattConnectionState.ReceivingReadings
        )

        assertEquals("Receiving readings", decentScaleCandidateActionLabel(gattState))
        assertFalse(isDecentScaleCandidateConnectEnabled(gattState))
    }
}
