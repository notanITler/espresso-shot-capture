package com.example.espressoshotcapture.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureTargetStateTest {
    @Test
    fun defaultTargetStateUsesMvpValues() {
        val targetState = MvpShotTarget.defaultState()

        assertEquals(18.0, targetState.doseGrams, 0.0)
        assertEquals(36.0, targetState.targetYieldGrams, 0.0)
        assertEquals(2.0, targetState.ratio ?: error("Expected ratio"), 0.0)
        assertTrue(targetState.isValid)
    }

    @Test
    fun ratioIsCalculatedFromTargetYieldAndDose() {
        val targetState = CaptureTargetState(
            doseGrams = 20.0,
            targetYieldGrams = 50.0
        )

        assertEquals(2.5, targetState.ratio ?: error("Expected ratio"), 0.0)
    }

    @Test
    fun invalidDoseMakesTargetInvalid() {
        val targetState = CaptureTargetState(
            doseGrams = 0.0,
            targetYieldGrams = 36.0
        )

        assertFalse(targetState.isValid)
        assertNull(targetState.ratio)
        assertNull(targetState.toCaptureTargetOrNull())
    }

    @Test
    fun invalidTargetYieldMakesTargetInvalid() {
        val targetState = CaptureTargetState(
            doseGrams = 18.0,
            targetYieldGrams = 0.0
        )

        assertFalse(targetState.isValid)
        assertNull(targetState.ratio)
        assertNull(targetState.toCaptureTargetOrNull())
    }

    @Test
    fun validTargetStateConvertsToCaptureTarget() {
        val captureTarget = CaptureTargetState(
            doseGrams = 18.0,
            targetYieldGrams = 36.0
        ).toCaptureTargetOrNull() ?: error("Expected capture target")

        assertEquals(18.0, captureTarget.doseG, 0.0)
        assertEquals(36.0, captureTarget.targetYieldG, 0.0)
        assertEquals(2.0, captureTarget.targetRatio, 0.0)
    }
}
