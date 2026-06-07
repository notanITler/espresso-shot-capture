package com.example.espressoshotcapture.capture.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScaleReadingMapperTest {
    @Test
    fun preservesTimestamp() {
        val sample = ScaleReadingMapper.toWeightSample(
            ScaleReading(timestampMillis = 12_345L, weightGrams = 18.5)
        )

        assertEquals(12_345L, sample.timestampMs)
    }

    @Test
    fun preservesWeight() {
        val sample = ScaleReadingMapper.toWeightSample(
            ScaleReading(timestampMillis = 12_345L, weightGrams = 18.5)
        )

        assertEquals(18.5, sample.weightG, 0.0)
    }
}
