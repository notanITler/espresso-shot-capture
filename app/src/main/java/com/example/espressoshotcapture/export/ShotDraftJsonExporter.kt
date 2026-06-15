package com.example.espressoshotcapture.export

import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.CapturedSample
import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.capture.domain.ShotMetadata
import com.example.espressoshotcapture.capture.domain.ShotResult
import com.example.espressoshotcapture.capture.domain.ShotTiming
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ShotDraftJsonExporter {
    fun export(shotDraft: ShotDraft): String =
        Json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("schemaVersion", 1)
                put("shot", shotDraft.toJson())
            }
        )

    private fun ShotDraft.toJson(): JsonObject =
        buildJsonObject {
            put("id", id)
            put("createdAtEpochMs", createdAtEpochMs)
            put("metadata", metadata.toJson())
            put("target", target.toJson())
            put("timing", timing.toJson())
            put("result", result.toJson())
            put(
                "samples",
                buildJsonArray {
                    samples.forEach { sample ->
                        add(sample.toJson())
                    }
                }
            )
            put("status", status.name)
            putNullable("notes", notes)
        }

    private fun ShotMetadata.toJson(): JsonObject =
        buildJsonObject {
            put("scaleSource", scaleSource.name)
        }

    private fun CaptureTarget.toJson(): JsonObject =
        buildJsonObject {
            put("source", source.name)
            putNullable("recipeId", recipeId)
            putNullable("beanId", beanId)
            put("doseG", doseG)
            put("targetRatio", targetRatio)
            put("targetYieldG", targetYieldG)
            putNullable("targetTimeS", targetTimeS)
        }

    private fun ShotTiming.toJson(): JsonObject =
        buildJsonObject {
            put("startMode", startMode.name)
            putNullable("stopMode", stopMode?.name)
            putNullable("brewTimeMs", brewTimeMs)
            putNullable("flowTimeMs", flowTimeMs)
            putNullable("targetReachedAtMs", targetReachedAtMs)
            putNullable("firstWeightDelayMs", firstWeightDelayMs)
            put("postTargetRecordingMs", postTargetRecordingMs)
        }

    private fun ShotResult.toJson(): JsonObject =
        buildJsonObject {
            putNullable("actualYieldG", actualYieldG)
            putNullable("postTargetDriftG", postTargetDriftG)
            putNullable("averageFlowGPerS", averageFlowGPerS)
            putNullable("maxFlowGPerS", maxFlowGPerS)
            put("sampleCount", sampleCount)
        }

    private fun CapturedSample.toJson(): JsonObject =
        buildJsonObject {
            put("index", index)
            put("tMs", tMs)
            put("weightGRaw", weightGRaw)
            putNullable("weightGFiltered", weightGFiltered)
            putNullable("flowGPerS", flowGPerS)
            put("isOutlier", isOutlier)
            put("source", source.name)
        }

    private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
        put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: Long?) {
        put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: Double?) {
        put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
    }
}
