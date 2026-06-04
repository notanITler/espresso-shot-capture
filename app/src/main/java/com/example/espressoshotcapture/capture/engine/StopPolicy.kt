package com.example.espressoshotcapture.capture.engine

enum class StopDecision {
    CONTINUE,
    TARGET_REACHED,
    COMPLETE
}

object StopPolicy {
    fun evaluate(
        currentWeightG: Double,
        targetYieldG: Double,
        targetReachedAtMs: Long?,
        currentTimestampMs: Long,
        config: ShotCaptureConfig = ShotCaptureConfig()
    ): StopDecision {
        if (currentWeightG < targetYieldG) {
            return StopDecision.CONTINUE
        }

        if (targetReachedAtMs == null) {
            return StopDecision.TARGET_REACHED
        }

        val elapsedMs = currentTimestampMs - targetReachedAtMs
        if (elapsedMs < 0) {
            return StopDecision.CONTINUE
        }

        return if (elapsedMs < config.postTargetRecordingMs) {
            StopDecision.CONTINUE
        } else {
            StopDecision.COMPLETE
        }
    }
}
