package com.example.espressoshotcapture.repository

import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.CapturedSample
import com.example.espressoshotcapture.capture.domain.SampleSource
import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.capture.domain.ShotResult
import com.example.espressoshotcapture.capture.domain.ShotSource
import com.example.espressoshotcapture.capture.domain.ShotStatus
import com.example.espressoshotcapture.capture.domain.ShotTiming
import com.example.espressoshotcapture.capture.domain.StartMode
import com.example.espressoshotcapture.capture.domain.StopMode
import com.example.espressoshotcapture.export.ShotDraftJsonExporter
import org.junit.Assert.assertEquals
import org.junit.Test

class ShotEntityMapperTest {
    @Test
    fun preservesGeneratedShotId() {
        val draft = sampleDraft(id = "shot-1234")

        val entity = ShotEntityMapper.fromShotDraft(draft)

        assertEquals("shot-1234", entity.id)
    }

    @Test
    fun preservesCreatedAtEpochMillis() {
        val draft = sampleDraft(createdAtEpochMs = 12_345L)

        val entity = ShotEntityMapper.fromShotDraft(draft)

        assertEquals(12_345L, entity.createdAtEpochMillis)
    }

    @Test
    fun storesCanonicalJsonExportPayload() {
        val draft = sampleDraft(id = "shot-1234")

        val entity = ShotEntityMapper.fromShotDraft(draft)

        assertEquals(ShotDraftJsonExporter.export(draft), entity.json)
    }

    private fun sampleDraft(
        id: String = "shot-1000",
        createdAtEpochMs: Long = 1_000L
    ): ShotDraft =
        ShotDraft(
            id = id,
            createdAtEpochMs = createdAtEpochMs,
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
                brewTimeMs = null,
                flowTimeMs = 25_000L,
                targetReachedAtMs = 24_000L,
                firstWeightDelayMs = null,
                postTargetRecordingMs = 1_500L
            ),
            result = ShotResult(
                actualYieldG = 37.25,
                postTargetDriftG = 1.25,
                averageFlowGPerS = null,
                maxFlowGPerS = null,
                sampleCount = 1
            ),
            samples = listOf(
                CapturedSample(
                    index = 0,
                    tMs = 25_000L,
                    weightGRaw = 37.25,
                    weightGFiltered = 37.25,
                    flowGPerS = null,
                    isOutlier = false,
                    source = SampleSource.SCALE
                )
            ),
            status = ShotStatus.COMPLETED,
            notes = null
        )
}
