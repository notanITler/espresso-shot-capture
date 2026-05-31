package com.example.espressoshotcapture.capture.domain

enum class ShotSource {
    QUICK_SHOT,
    RECIPE
}

enum class StartMode {
    AUTO_WEIGHT
}

enum class StopMode {
    TARGET_YIELD,
    MANUAL,
    INTERRUPTED
}

enum class ShotStatus {
    COMPLETED,
    MANUAL_STOPPED,
    INTERRUPTED,
    ERROR
}

enum class SampleSource {
    SCALE
}

data class WeightSample(
    val timestampMs: Long,
    val weightG: Double,
    val source: SampleSource = SampleSource.SCALE
)

data class CaptureTarget(
    val source: ShotSource,
    val recipeId: String?,
    val beanId: String?,
    val doseG: Double,
    val targetRatio: Double,
    val targetYieldG: Double,
    val targetTimeS: Double?
)

data class CapturedSample(
    val index: Int,
    val tMs: Long,
    val weightGRaw: Double,
    val weightGFiltered: Double?,
    val flowGPerS: Double?,
    val isOutlier: Boolean,
    val source: SampleSource = SampleSource.SCALE
)

data class ShotTiming(
    val startMode: StartMode,
    val stopMode: StopMode?,
    val brewTimeMs: Long?,
    val flowTimeMs: Long?,
    val targetReachedAtMs: Long?,
    val firstWeightDelayMs: Long?,
    val postTargetRecordingMs: Long
)

data class ShotResult(
    val actualYieldG: Double?,
    val postTargetDriftG: Double?,
    val averageFlowGPerS: Double?,
    val maxFlowGPerS: Double?,
    val sampleCount: Int
)

data class ShotDraft(
    val id: String,
    val createdAtEpochMs: Long,
    val target: CaptureTarget,
    val timing: ShotTiming,
    val result: ShotResult,
    val samples: List<CapturedSample>,
    val status: ShotStatus,
    val notes: String?
)

