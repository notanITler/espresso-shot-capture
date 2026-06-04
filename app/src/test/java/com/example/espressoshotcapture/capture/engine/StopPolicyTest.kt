package com.example.espressoshotcapture.capture.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class StopPolicyTest {
    @Test
    fun continuesBeforeTargetReached() {
        assertEquals(
            StopDecision.CONTINUE,
            StopPolicy.evaluate(
                currentWeightG = 17.9,
                targetYieldG = 18.0,
                targetReachedAtMs = null,
                currentTimestampMs = 1000
            )
        )
    }

    @Test
    fun emitsTargetReachedWhenCrossingTarget() {
        assertEquals(
            StopDecision.TARGET_REACHED,
            StopPolicy.evaluate(
                currentWeightG = 18.0,
                targetYieldG = 18.0,
                targetReachedAtMs = null,
                currentTimestampMs = 1000
            )
        )
    }

    @Test
    fun continuesDuringPostTargetWindow() {
        assertEquals(
            StopDecision.CONTINUE,
            StopPolicy.evaluate(
                currentWeightG = 19.0,
                targetYieldG = 18.0,
                targetReachedAtMs = 1000,
                currentTimestampMs = 2499
            )
        )
    }

    @Test
    fun completesAfterPostTargetWindow() {
        assertEquals(
            StopDecision.COMPLETE,
            StopPolicy.evaluate(
                currentWeightG = 19.0,
                targetYieldG = 18.0,
                targetReachedAtMs = 1000,
                currentTimestampMs = 2500
            )
        )
    }

    @Test
    fun usesCustomPostTargetRecordingWindow() {
        val config = ShotCaptureConfig(postTargetRecordingMs = 2500)

        assertEquals(
            StopDecision.CONTINUE,
            StopPolicy.evaluate(
                currentWeightG = 19.0,
                targetYieldG = 18.0,
                targetReachedAtMs = 1000,
                currentTimestampMs = 3499,
                config = config
            )
        )
        assertEquals(
            StopDecision.COMPLETE,
            StopPolicy.evaluate(
                currentWeightG = 19.0,
                targetYieldG = 18.0,
                targetReachedAtMs = 1000,
                currentTimestampMs = 3500,
                config = config
            )
        )
    }

    @Test
    fun handlesNegativeElapsedTimeGracefully() {
        assertEquals(
            StopDecision.CONTINUE,
            StopPolicy.evaluate(
                currentWeightG = 19.0,
                targetYieldG = 18.0,
                targetReachedAtMs = 2000,
                currentTimestampMs = 1000
            )
        )
    }
}
