package com.example.espressoshotcapture.capture.engine

data class ShotCaptureConfig(
    val startMinWeightG: Double = 0.3,
    val startConfirmationSamples: Int = 3,
    val startMinFlowGPerS: Double = 0.2,
    val prebufferMs: Long = 1000,
    val postTargetRecordingMs: Long = 1500,
    val outlierJumpThresholdG: Double = 5.0
)
