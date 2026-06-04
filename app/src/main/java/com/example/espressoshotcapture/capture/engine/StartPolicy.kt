package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.WeightSample

object StartPolicy {
    fun shouldStart(
        state: ShotCaptureState,
        recentSamples: List<WeightSample>,
        config: ShotCaptureConfig = ShotCaptureConfig()
    ): Boolean {
        if (state != ShotCaptureState.ARMED) {
            return false
        }

        val confirmationSampleCount = config.startConfirmationSamples
        if (confirmationSampleCount <= 0 || recentSamples.size < confirmationSampleCount) {
            return false
        }

        val confirmationSamples = recentSamples.takeLast(confirmationSampleCount)
        val latestSample = confirmationSamples.last()
        if (latestSample.weightG < config.startMinWeightG) {
            return false
        }

        val samplePairs = confirmationSamples.zipWithNext()
        if (samplePairs.any { (previous, current) -> current.weightG < previous.weightG }) {
            return false
        }
        if (samplePairs.none { (previous, current) -> current.weightG > previous.weightG }) {
            return false
        }

        val firstSample = confirmationSamples.first()
        val timeDeltaMs = latestSample.timestampMs - firstSample.timestampMs
        if (timeDeltaMs <= 0) {
            return false
        }

        val weightDeltaG = latestSample.weightG - firstSample.weightG
        val flowGPerS = weightDeltaG / (timeDeltaMs / MILLIS_PER_SECOND)
        return flowGPerS >= config.startMinFlowGPerS
    }

    private const val MILLIS_PER_SECOND = 1000.0
}
