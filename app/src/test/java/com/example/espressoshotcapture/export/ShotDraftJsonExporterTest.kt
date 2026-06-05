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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotDraftJsonExporterTest {
    @Test
    fun exportsSchemaVersion() {
        val json = exportJson()

        assertEquals(1, json["schemaVersion"]?.jsonPrimitive?.int)
    }

    @Test
    fun exportsShotRootObject() {
        val json = exportJson()
        val shot = json["shot"]!!.jsonObject

        assertTrue(json["shot"] is JsonObject)
        assertEquals("shot-1000", shot["id"]?.jsonPrimitive?.content)
        assertEquals(1000L, shot["createdAtEpochMs"]?.jsonPrimitive?.long)
        assertEquals("COMPLETED", shot["status"]?.jsonPrimitive?.content)
        assertEquals("Balanced and sweet", shot["notes"]?.jsonPrimitive?.content)
    }

    @Test
    fun exportsTargetInformation() {
        val target = exportShot()["target"]!!.jsonObject

        assertEquals("QUICK_SHOT", target["source"]?.jsonPrimitive?.content)
        assertEquals(18.0, target["doseG"]?.jsonPrimitive?.double ?: error("Expected dose"), 0.0)
        assertEquals(2.0, target["targetRatio"]?.jsonPrimitive?.double ?: error("Expected ratio"), 0.0)
        assertEquals(36.5, target["targetYieldG"]?.jsonPrimitive?.double ?: error("Expected yield"), 0.0)
    }

    @Test
    fun exportsTimingInformation() {
        val timing = exportShot()["timing"]!!.jsonObject

        assertEquals("AUTO_WEIGHT", timing["startMode"]?.jsonPrimitive?.content)
        assertEquals("TARGET_YIELD", timing["stopMode"]?.jsonPrimitive?.content)
        assertEquals(28_000L, timing["brewTimeMs"]?.jsonPrimitive?.long)
        assertEquals(25_000L, timing["flowTimeMs"]?.jsonPrimitive?.long)
        assertEquals(24_000L, timing["targetReachedAtMs"]?.jsonPrimitive?.long)
        assertEquals(3_000L, timing["firstWeightDelayMs"]?.jsonPrimitive?.long)
        assertEquals(1_500L, timing["postTargetRecordingMs"]?.jsonPrimitive?.long)
    }

    @Test
    fun exportsResultInformation() {
        val result = exportShot()["result"]!!.jsonObject

        assertEquals(37.25, result["actualYieldG"]?.jsonPrimitive?.double ?: error("Expected actual yield"), 0.0)
        assertEquals(0.75, result["postTargetDriftG"]?.jsonPrimitive?.double ?: error("Expected drift"), 0.0)
        assertEquals(1.49, result["averageFlowGPerS"]?.jsonPrimitive?.double ?: error("Expected average flow"), 0.0)
        assertEquals(2.12, result["maxFlowGPerS"]?.jsonPrimitive?.double ?: error("Expected max flow"), 0.0)
        assertEquals(2, result["sampleCount"]?.jsonPrimitive?.int)
    }

    @Test
    fun exportsSamples() {
        val samples = exportShot()["samples"] as JsonArray
        val firstSample = samples[0].jsonObject
        val secondSample = samples[1].jsonObject

        assertEquals(2, samples.size)
        assertEquals(0, firstSample["index"]?.jsonPrimitive?.int)
        assertEquals(250L, firstSample["tMs"]?.jsonPrimitive?.long)
        assertEquals(1.25, firstSample["weightGRaw"]?.jsonPrimitive?.double ?: error("Expected raw weight"), 0.0)
        assertEquals(1.25, firstSample["weightGFiltered"]?.jsonPrimitive?.double ?: error("Expected filtered weight"), 0.0)
        assertEquals(false, firstSample["isOutlier"]?.jsonPrimitive?.boolean)
        assertEquals("SCALE", firstSample["source"]?.jsonPrimitive?.content)
        assertEquals(1, secondSample["index"]?.jsonPrimitive?.int)
        assertEquals(true, secondSample["isOutlier"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun exportsNullValues() {
        val shot = exportShot(nullableDraft())
        val target = shot["target"]!!.jsonObject
        val timing = shot["timing"]!!.jsonObject
        val result = shot["result"]!!.jsonObject
        val sample = (shot["samples"] as JsonArray)[0].jsonObject

        assertEquals(JsonNull, target["recipeId"])
        assertEquals(JsonNull, target["beanId"])
        assertEquals(JsonNull, target["targetTimeS"])
        assertEquals(JsonNull, timing["stopMode"])
        assertEquals(JsonNull, timing["brewTimeMs"])
        assertEquals(JsonNull, result["averageFlowGPerS"])
        assertEquals(JsonNull, sample["weightGFiltered"])
        assertEquals(JsonNull, sample["flowGPerS"])
        assertEquals(JsonNull, shot["notes"])
    }

    @Test
    fun exportIsDeterministic() {
        val draft = sampleDraft()

        assertEquals(
            ShotDraftJsonExporter.export(draft),
            ShotDraftJsonExporter.export(draft)
        )
    }

    private fun exportJson(draft: ShotDraft = sampleDraft()): JsonObject =
        Json.parseToJsonElement(ShotDraftJsonExporter.export(draft)).jsonObject

    private fun exportShot(draft: ShotDraft = sampleDraft()): JsonObject =
        exportJson(draft)["shot"]!!.jsonObject

    private fun sampleDraft(): ShotDraft =
        ShotDraft(
            id = "shot-1000",
            createdAtEpochMs = 1000L,
            target = CaptureTarget(
                source = ShotSource.QUICK_SHOT,
                recipeId = "recipe-1",
                beanId = "bean-1",
                doseG = 18.0,
                targetRatio = 2.0,
                targetYieldG = 36.5,
                targetTimeS = 28.5
            ),
            timing = ShotTiming(
                startMode = StartMode.AUTO_WEIGHT,
                stopMode = StopMode.TARGET_YIELD,
                brewTimeMs = 28_000L,
                flowTimeMs = 25_000L,
                targetReachedAtMs = 24_000L,
                firstWeightDelayMs = 3_000L,
                postTargetRecordingMs = 1_500L
            ),
            result = ShotResult(
                actualYieldG = 37.25,
                postTargetDriftG = 0.75,
                averageFlowGPerS = 1.49,
                maxFlowGPerS = 2.12,
                sampleCount = 2
            ),
            samples = listOf(
                CapturedSample(
                    index = 0,
                    tMs = 250L,
                    weightGRaw = 1.25,
                    weightGFiltered = 1.25,
                    flowGPerS = null,
                    isOutlier = false,
                    source = SampleSource.SCALE
                ),
                CapturedSample(
                    index = 1,
                    tMs = 500L,
                    weightGRaw = 2.75,
                    weightGFiltered = 2.75,
                    flowGPerS = null,
                    isOutlier = true,
                    source = SampleSource.SCALE
                )
            ),
            status = ShotStatus.COMPLETED,
            notes = "Balanced and sweet"
        )

    private fun nullableDraft(): ShotDraft =
        sampleDraft().copy(
            target = sampleDraft().target.copy(
                recipeId = null,
                beanId = null,
                targetTimeS = null
            ),
            timing = sampleDraft().timing.copy(
                stopMode = null,
                brewTimeMs = null
            ),
            result = sampleDraft().result.copy(
                averageFlowGPerS = null
            ),
            samples = listOf(
                sampleDraft().samples.first().copy(
                    weightGFiltered = null,
                    flowGPerS = null
                )
            ),
            notes = null
        )
}
