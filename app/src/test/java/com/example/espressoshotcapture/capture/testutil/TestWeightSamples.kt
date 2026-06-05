package com.example.espressoshotcapture.capture.testutil

import com.example.espressoshotcapture.capture.domain.WeightSample

class TestWeightSamples(
    private val startTimestampMs: Long = 0L
) {
    private var currentTimestampMs: Long = startTimestampMs

    fun emit(weightG: Double): WeightSample =
        emit(weightG = weightG, deltaMs = DEFAULT_STEP_MS)

    fun emit(weightG: Double, deltaMs: Long): WeightSample {
        val sample = WeightSample(
            timestampMs = currentTimestampMs,
            weightG = weightG
        )
        currentTimestampMs += deltaMs
        return sample
    }

    fun reset() {
        currentTimestampMs = startTimestampMs
    }

    private companion object {
        const val DEFAULT_STEP_MS = 100L
    }
}
