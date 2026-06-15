package com.example.espressoshotcapture.capture

import com.example.espressoshotcapture.capture.domain.CapturedSample
import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.capture.domain.ShotMetadata
import com.example.espressoshotcapture.capture.domain.ShotResult
import com.example.espressoshotcapture.capture.domain.ShotScaleSource
import com.example.espressoshotcapture.capture.domain.ShotStatus
import com.example.espressoshotcapture.capture.domain.ShotTiming
import com.example.espressoshotcapture.capture.domain.StartMode
import com.example.espressoshotcapture.capture.domain.StopMode

object FakeCaptureShotDraftFactory {
    fun create(createdAtEpochMs: Long): ShotDraft {
        val target = MvpShotTarget.toCaptureTarget()
        val actualYieldG = 36.8
        val samples = listOf(
            CapturedSample(
                index = 0,
                tMs = 0L,
                weightGRaw = 0.4,
                weightGFiltered = 0.4,
                flowGPerS = null,
                isOutlier = false
            ),
            CapturedSample(
                index = 1,
                tMs = 14_000L,
                weightGRaw = 18.2,
                weightGFiltered = 18.2,
                flowGPerS = null,
                isOutlier = false
            ),
            CapturedSample(
                index = 2,
                tMs = 28_000L,
                weightGRaw = actualYieldG,
                weightGFiltered = actualYieldG,
                flowGPerS = null,
                isOutlier = false
            )
        )

        return ShotDraft(
            id = "fake-shot-$createdAtEpochMs",
            createdAtEpochMs = createdAtEpochMs,
            target = target,
            timing = ShotTiming(
                startMode = StartMode.AUTO_WEIGHT,
                stopMode = StopMode.MANUAL,
                brewTimeMs = null,
                flowTimeMs = 28_000L,
                targetReachedAtMs = 27_400L,
                firstWeightDelayMs = null,
                postTargetRecordingMs = 1_500L
            ),
            result = ShotResult(
                actualYieldG = actualYieldG,
                postTargetDriftG = actualYieldG - target.targetYieldG,
                averageFlowGPerS = 1.3,
                maxFlowGPerS = null,
                sampleCount = samples.size
            ),
            samples = samples,
            status = ShotStatus.MANUAL_STOPPED,
            notes = "Manual fake capture",
            metadata = ShotMetadata(scaleSource = ShotScaleSource.FAKE_DEMO)
        )
    }
}
