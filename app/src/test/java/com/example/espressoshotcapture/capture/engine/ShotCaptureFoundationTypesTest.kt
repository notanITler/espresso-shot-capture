package com.example.espressoshotcapture.capture.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ShotCaptureFoundationTypesTest {
    @Test
    fun configUsesExpectedDefaults() {
        val config = ShotCaptureConfig()

        assertEquals(0.3, config.startMinWeightG, 0.0)
        assertEquals(3, config.startConfirmationSamples)
        assertEquals(0.2, config.startMinFlowGPerS, 0.0)
        assertEquals(1000, config.prebufferMs)
        assertEquals(1500, config.postTargetRecordingMs)
        assertEquals(5.0, config.outlierJumpThresholdG, 0.0)
    }

    @Test
    fun stateExposesExpectedValues() {
        assertEquals(
            listOf(
                ShotCaptureState.DISCONNECTED,
                ShotCaptureState.CONNECTED_IDLE,
                ShotCaptureState.TARED,
                ShotCaptureState.ARMED,
                ShotCaptureState.RECORDING,
                ShotCaptureState.TARGET_REACHED,
                ShotCaptureState.SAVED,
                ShotCaptureState.ERROR
            ),
            ShotCaptureState.entries.toList()
        )
    }
}
