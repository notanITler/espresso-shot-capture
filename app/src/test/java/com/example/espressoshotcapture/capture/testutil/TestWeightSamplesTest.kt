package com.example.espressoshotcapture.capture.testutil

import com.example.espressoshotcapture.capture.domain.SampleSource
import org.junit.Assert.assertEquals
import org.junit.Test

class TestWeightSamplesTest {
    @Test
    fun emitsIncreasingTimestamps() {
        val samples = TestWeightSamples(startTimestampMs = 1000L)

        assertEquals(1000L, samples.emit(weightG = 1.0).timestampMs)
        assertEquals(1100L, samples.emit(weightG = 2.0).timestampMs)
        assertEquals(1200L, samples.emit(weightG = 3.0).timestampMs)
    }

    @Test
    fun emitWithCustomDeltaAdvancesTime() {
        val samples = TestWeightSamples(startTimestampMs = 1000L)

        assertEquals(1000L, samples.emit(weightG = 1.0, deltaMs = 250L).timestampMs)
        assertEquals(1250L, samples.emit(weightG = 2.0, deltaMs = 50L).timestampMs)
        assertEquals(1300L, samples.emit(weightG = 3.0).timestampMs)
    }

    @Test
    fun resetRestoresInitialTimestamp() {
        val samples = TestWeightSamples(startTimestampMs = 1000L)
        samples.emit(weightG = 1.0)
        samples.emit(weightG = 2.0)

        samples.reset()

        assertEquals(1000L, samples.emit(weightG = 3.0).timestampMs)
    }

    @Test
    fun preservesWeightValues() {
        val samples = TestWeightSamples()

        val sample = samples.emit(weightG = 18.75)

        assertEquals(18.75, sample.weightG, 0.0)
        assertEquals(SampleSource.SCALE, sample.source)
    }
}
