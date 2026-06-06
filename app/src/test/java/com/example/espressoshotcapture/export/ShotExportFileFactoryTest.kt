package com.example.espressoshotcapture.export

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotExportFileFactoryTest {
    @Test
    fun createsJsonFileNameFromShotId() {
        val file = ShotExportFileFactory.create(sampleDraft(id = "abc123"))

        assertEquals("shot-abc123.json", file.fileName)
    }

    @Test
    fun usesApplicationJsonMimeType() {
        val file = ShotExportFileFactory.create(sampleDraft())

        assertEquals("application/json", file.mimeType)
    }

    @Test
    fun fileContentMatchesShotDraftJsonExporter() {
        val draft = sampleDraft()
        val file = ShotExportFileFactory.create(draft)

        assertEquals(ShotDraftJsonExporter.export(draft), file.content)
    }

    @Test
    fun doesNotModifyJsonContract() {
        val file = ShotExportFileFactory.create(sampleDraft())
        val json = Json.parseToJsonElement(file.content).jsonObject

        assertEquals(setOf("schemaVersion", "shot"), json.keys)
        assertEquals(1, json["schemaVersion"]?.jsonPrimitive?.int)
        assertTrue(json["shot"] is JsonObject)
    }

    private fun sampleDraft(id: String = "shot-1000"): ShotDraft =
        ShotDraft(
            id = id,
            createdAtEpochMs = 1_000L,
            target = CaptureTarget(
                source = ShotSource.QUICK_SHOT,
                recipeId = "recipe-1",
                beanId = "bean-1",
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
                sampleCount = 3
            ),
            samples = listOf(
                CapturedSample(
                    index = 0,
                    tMs = 0L,
                    weightGRaw = 0.15,
                    weightGFiltered = 0.15,
                    flowGPerS = null,
                    isOutlier = false,
                    source = SampleSource.SCALE
                ),
                CapturedSample(
                    index = 1,
                    tMs = 12_000L,
                    weightGRaw = 18.5,
                    weightGFiltered = 18.5,
                    flowGPerS = null,
                    isOutlier = false,
                    source = SampleSource.SCALE
                ),
                CapturedSample(
                    index = 2,
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
