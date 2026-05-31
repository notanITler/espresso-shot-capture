package com.example.espressoshotcapture.capture.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureModelsTest {
    @Test
    fun shotDraftPreservesCaptureValues() {
        val sample = CapturedSample(
            index = 0,
            tMs = 250,
            weightGRaw = 1.7,
            weightGFiltered = null,
            flowGPerS = null,
            isOutlier = false
        )
        val draft = ShotDraft(
            id = "shot-1",
            createdAtEpochMs = 1_700_000_000_000,
            target = CaptureTarget(
                source = ShotSource.QUICK_SHOT,
                recipeId = null,
                beanId = null,
                doseG = 18.0,
                targetRatio = 2.0,
                targetYieldG = 36.0,
                targetTimeS = null
            ),
            timing = ShotTiming(
                startMode = StartMode.AUTO_WEIGHT,
                stopMode = StopMode.TARGET_YIELD,
                brewTimeMs = 28_000,
                flowTimeMs = 25_000,
                targetReachedAtMs = 27_000,
                firstWeightDelayMs = 3_000,
                postTargetRecordingMs = 1_000
            ),
            result = ShotResult(
                actualYieldG = 36.4,
                postTargetDriftG = 0.4,
                averageFlowGPerS = 1.44,
                maxFlowGPerS = 2.1,
                sampleCount = 1
            ),
            samples = listOf(sample),
            status = ShotStatus.COMPLETED,
            notes = null
        )

        assertEquals(36.0, draft.target.targetYieldG, 0.0)
        assertEquals(1, draft.result.sampleCount)
        assertEquals(1.7, draft.samples.single().weightGRaw, 0.0)
    }
}

