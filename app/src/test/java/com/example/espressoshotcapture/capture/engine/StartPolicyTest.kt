package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.WeightSample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartPolicyTest {
    @Test
    fun doesNotStartWhenStateIsNotArmed() {
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.TARED,
                recentSamples = samples(0.1, 0.3, 0.5)
            )
        )
    }

    @Test
    fun doesNotStartWithTooFewSamples() {
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.2, 0.4)
            )
        )
    }

    @Test
    fun doesNotStartBelowMinimumWeight() {
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.0, 0.1, 0.2)
            )
        )
    }

    @Test
    fun doesNotStartOnFlatSamples() {
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.4, 0.4, 0.4)
            )
        )
    }

    @Test
    fun doesNotStartOnSingleSpikeFollowedByDrop() {
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.1, 0.8, 0.5)
            )
        )
    }

    @Test
    fun doesNotStartOnWeightTouch() {
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.0, 0.4, 0.0)
            )
        )
    }

    @Test
    fun startsOnStablePositiveWeightIncrease() {
        assertTrue(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.1, 0.3, 0.5)
            )
        )
    }

    @Test
    fun usesCustomConfigThresholds() {
        val config = ShotCaptureConfig(
            startMinWeightG = 1.0,
            startConfirmationSamples = 4,
            startMinFlowGPerS = 2.0
        )

        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.3, 0.6, 0.9),
                config = config
            )
        )
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.3, 0.5, 0.7, 0.9),
                config = config
            )
        )
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.3, 0.5, 0.8, 1.0),
                config = config
            )
        )
        assertTrue(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samples(0.3, 0.5, 0.8, 1.1, intervalMs = 100),
                config = config
            )
        )
    }

    @Test
    fun doesNotStartWhenTimeDeltaIsZeroOrNegative() {
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samplesAt(
                    1000L to 0.1,
                    1000L to 0.3,
                    1000L to 0.5
                )
            )
        )
        assertFalse(
            StartPolicy.shouldStart(
                state = ShotCaptureState.ARMED,
                recentSamples = samplesAt(
                    3000L to 0.1,
                    2000L to 0.3,
                    1000L to 0.5
                )
            )
        )
    }

    private fun samples(vararg weightsG: Double, intervalMs: Long = 1000): List<WeightSample> =
        weightsG.mapIndexed { index, weightG ->
            WeightSample(
                timestampMs = index * intervalMs,
                weightG = weightG
            )
        }

    private fun samplesAt(vararg samples: Pair<Long, Double>): List<WeightSample> =
        samples.map { (timestampMs, weightG) ->
            WeightSample(
                timestampMs = timestampMs,
                weightG = weightG
            )
        }
}
