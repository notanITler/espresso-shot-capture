package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.CapturedSample
import com.example.espressoshotcapture.capture.domain.WeightSample
import kotlin.math.abs

object SamplingPolicy {
    fun toCapturedSample(
        index: Int,
        recordingStartTimestampMs: Long,
        currentSample: WeightSample,
        previousSample: WeightSample?,
        config: ShotCaptureConfig = ShotCaptureConfig()
    ): CapturedSample {
        val isOutlier = previousSample != null &&
            abs(currentSample.weightG - previousSample.weightG) > config.outlierJumpThresholdG

        return CapturedSample(
            index = index,
            tMs = currentSample.timestampMs - recordingStartTimestampMs,
            weightGRaw = currentSample.weightG,
            weightGFiltered = currentSample.weightG,
            flowGPerS = null,
            isOutlier = isOutlier,
            source = currentSample.source
        )
    }
}
