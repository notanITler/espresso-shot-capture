package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.WeightSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplingPolicyTest {
    @Test
    fun createsCapturedSampleWithRelativeTime() {
        val capturedSample = SamplingPolicy.toCapturedSample(
            index = 4,
            recordingStartTimestampMs = 1000,
            currentSample = sample(timestampMs = 1750, weightG = 8.5),
            previousSample = null
        )

        assertEquals(4, capturedSample.index)
        assertEquals(750, capturedSample.tMs)
    }

    @Test
    fun preservesRawWeight() {
        val capturedSample = SamplingPolicy.toCapturedSample(
            index = 0,
            recordingStartTimestampMs = 1000,
            currentSample = sample(timestampMs = 1100, weightG = 12.34),
            previousSample = null
        )

        assertEquals(12.34, capturedSample.weightGRaw, 0.0)
    }

    @Test
    fun usesRawWeightAsFilteredWeightForV1() {
        val capturedSample = SamplingPolicy.toCapturedSample(
            index = 0,
            recordingStartTimestampMs = 1000,
            currentSample = sample(timestampMs = 1100, weightG = 12.34),
            previousSample = null
        )

        assertEquals(12.34, capturedSample.weightGFiltered ?: error("Expected filtered weight"), 0.0)
    }

    @Test
    fun flowIsNullForV1() {
        val capturedSample = SamplingPolicy.toCapturedSample(
            index = 0,
            recordingStartTimestampMs = 1000,
            currentSample = sample(timestampMs = 1100, weightG = 12.34),
            previousSample = sample(timestampMs = 1000, weightG = 11.0)
        )

        assertNull(capturedSample.flowGPerS)
    }

    @Test
    fun firstSampleIsNotOutlier() {
        val capturedSample = SamplingPolicy.toCapturedSample(
            index = 0,
            recordingStartTimestampMs = 1000,
            currentSample = sample(timestampMs = 1000, weightG = 6.0),
            previousSample = null
        )

        assertFalse(capturedSample.isOutlier)
    }

    @Test
    fun marksLargeWeightJumpAsOutlier() {
        val capturedSample = SamplingPolicy.toCapturedSample(
            index = 1,
            recordingStartTimestampMs = 1000,
            currentSample = sample(timestampMs = 1100, weightG = 7.1),
            previousSample = sample(timestampMs = 1000, weightG = 2.0)
        )

        assertTrue(capturedSample.isOutlier)
    }

    @Test
    fun doesNotMarkSmallWeightChangeAsOutlier() {
        val capturedSample = SamplingPolicy.toCapturedSample(
            index = 1,
            recordingStartTimestampMs = 1000,
            currentSample = sample(timestampMs = 1100, weightG = 6.9),
            previousSample = sample(timestampMs = 1000, weightG = 2.0)
        )

        assertFalse(capturedSample.isOutlier)
    }

    @Test
    fun usesCustomOutlierThreshold() {
        val config = ShotCaptureConfig(outlierJumpThresholdG = 2.0)

        val capturedSample = SamplingPolicy.toCapturedSample(
            index = 1,
            recordingStartTimestampMs = 1000,
            currentSample = sample(timestampMs = 1100, weightG = 5.0),
            previousSample = sample(timestampMs = 1000, weightG = 2.9),
            config = config
        )

        assertTrue(capturedSample.isOutlier)
    }

    private fun sample(timestampMs: Long, weightG: Double): WeightSample =
        WeightSample(
            timestampMs = timestampMs,
            weightG = weightG
        )
}
