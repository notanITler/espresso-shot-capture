package com.example.espressoshotcapture.capture.domain

object ScaleReadingMapper {
    fun toWeightSample(reading: ScaleReading): WeightSample =
        WeightSample(
            timestampMs = reading.timestampMillis,
            weightG = reading.weightGrams
        )
}
