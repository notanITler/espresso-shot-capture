package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.roundToInt

object ShotHistoryMapper {
    const val UNKNOWN_YIELD_LABEL: String = "Yield: --"
    const val UNKNOWN_FLOW_TIME_LABEL: String = "Flow time: --"
    const val UNKNOWN_TARGET_YIELD_LABEL: String = "Target: --"
    const val UNKNOWN_AVERAGE_FLOW_LABEL: String = "Average flow: --"
    const val UNKNOWN_TARGET_REACHED_LABEL: String = "Target reached: --"
    const val UNKNOWN_SOURCE_LABEL: String = "Source: --"
    const val UNKNOWN_SAMPLE_COUNT_LABEL: String = "Samples: --"
    const val UNKNOWN_DOSE_LABEL: String = "Dose: --"
    const val UNKNOWN_QUALITY_LABEL: String = "Quality: --"

    fun fromEntity(entity: ShotEntity): ShotHistoryItem =
        summaryFromJson(entity.json).let { summary ->
            ShotHistoryItem(
                id = entity.id,
                createdAtEpochMillis = entity.createdAtEpochMillis,
                sourceLabel = summary.sourceLabel,
                qualityLabel = summary.qualityLabel,
                finalYieldLabel = summary.finalYieldLabel,
                flowTimeLabel = summary.flowTimeLabel,
                averageFlowLabel = summary.averageFlowLabel,
                sampleCountLabel = summary.sampleCountLabel,
                doseLabel = summary.doseLabel,
                targetYieldLabel = summary.targetYieldLabel
            )
        }

    fun fromEntities(entities: List<ShotEntity>): List<ShotHistoryItem> =
        entities.map(::fromEntity)

    fun summaryFromJson(json: String): ShotHistorySummary {
        val shot = runCatching {
            Json.parseToJsonElement(json).jsonObject["shot"]?.jsonObject
        }.getOrNull() ?: return ShotHistorySummary()

        val result = shot.objectAt("result")
        val timing = shot.objectAt("timing")
        val target = shot.objectAt("target")
        val metadata = shot.objectAt("metadata")
        val samples = shot.arrayAt("samples")
        val actualYieldG = shot.objectAt("result")?.doubleAt("actualYieldG")
            ?: samples
                ?.lastOrNull()
                ?.let { sample -> sample as? JsonObject }
                ?.doubleAt("weightGRaw")
        val flowTimeMs = timing?.longAt("flowTimeMs")
        val doseG = target?.doubleAt("doseG")
        val targetYieldG = target?.doubleAt("targetYieldG")
        val averageFlowGPerS = result?.doubleAt("averageFlowGPerS")
        val sampleCount = result?.longAt("sampleCount") ?: samples?.size?.toLong()
        val targetReachedLabel = timing?.let { timingObject ->
            if ("targetReachedAtMs" in timingObject) {
                if (timingObject.longAt("targetReachedAtMs") != null) {
                    "Target reached: yes"
                } else {
                    "Target reached: no"
                }
            } else {
                UNKNOWN_TARGET_REACHED_LABEL
            }
        } ?: UNKNOWN_TARGET_REACHED_LABEL
        val sourceLabel = when (metadata?.stringAt("scaleSource")) {
            "FAKE_DEMO" -> "Source: Fake/demo"
            "DECENT_SCALE" -> "Source: Decent Scale"
            else -> UNKNOWN_SOURCE_LABEL
        }
        val qualityLabel = when {
            sampleCount == 0L -> "Quality: No samples"
            flowTimeMs == 0L -> "Quality: Zero flow time"
            target == null || doseG == null || targetYieldG == null -> "Quality: Missing target"
            sampleCount != null && flowTimeMs != null -> "Quality: Complete"
            else -> UNKNOWN_QUALITY_LABEL
        }

        return ShotHistorySummary(
            sourceLabel = sourceLabel,
            qualityLabel = qualityLabel,
            finalYieldLabel = actualYieldG?.let { yield -> "Yield: ${yield.toOneDecimal()} g" }
                ?: UNKNOWN_YIELD_LABEL,
            flowTimeLabel = flowTimeMs?.let { timeMs -> "Flow time: ${timeMs / 1_000L} s" }
                ?: UNKNOWN_FLOW_TIME_LABEL,
            sampleCountLabel = sampleCount?.let { count -> "Samples: $count" }
                ?: UNKNOWN_SAMPLE_COUNT_LABEL,
            doseLabel = doseG?.let { dose -> "Dose: ${dose.toOneDecimal()} g" }
                ?: UNKNOWN_DOSE_LABEL,
            targetYieldLabel = targetYieldG?.let { target -> "Target: ${target.toOneDecimal()} g" }
                ?: UNKNOWN_TARGET_YIELD_LABEL,
            averageFlowLabel = averageFlowGPerS?.let { flow -> "Average flow: ${flow.toOneDecimal()} g/s" }
                ?: UNKNOWN_AVERAGE_FLOW_LABEL,
            targetReachedLabel = targetReachedLabel
        )
    }

    data class ShotHistorySummary(
        val sourceLabel: String = UNKNOWN_SOURCE_LABEL,
        val qualityLabel: String = UNKNOWN_QUALITY_LABEL,
        val finalYieldLabel: String = UNKNOWN_YIELD_LABEL,
        val flowTimeLabel: String = UNKNOWN_FLOW_TIME_LABEL,
        val sampleCountLabel: String = UNKNOWN_SAMPLE_COUNT_LABEL,
        val doseLabel: String = UNKNOWN_DOSE_LABEL,
        val targetYieldLabel: String = UNKNOWN_TARGET_YIELD_LABEL,
        val averageFlowLabel: String = UNKNOWN_AVERAGE_FLOW_LABEL,
        val targetReachedLabel: String = UNKNOWN_TARGET_REACHED_LABEL
    )

    private fun JsonObject.objectAt(key: String): JsonObject? =
        this[key] as? JsonObject

    private fun JsonObject.arrayAt(key: String): JsonArray? =
        this[key] as? JsonArray

    private fun JsonObject.doubleAt(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.longAt(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.stringAt(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun Double.toOneDecimal(): String =
        ((this * 10.0).roundToInt() / 10.0).toString()
}
