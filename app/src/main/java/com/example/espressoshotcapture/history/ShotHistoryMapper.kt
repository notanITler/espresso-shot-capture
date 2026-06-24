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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object ShotHistoryMapper {
    const val UNASSIGNED_BEAN_LABEL: String = "Unassigned bean"
    const val UNKNOWN_COMPARISON_METRICS_LABEL: String = "--"
    const val UNKNOWN_YIELD_LABEL: String = "Yield: --"
    const val UNKNOWN_FLOW_TIME_LABEL: String = "Flow time: --"
    const val UNKNOWN_TARGET_YIELD_LABEL: String = "Target: --"
    const val UNKNOWN_RATIO_LABEL: String = "Ratio: --"
    const val UNKNOWN_AVERAGE_FLOW_LABEL: String = "Average flow: --"
    const val UNKNOWN_TARGET_REACHED_LABEL: String = "Target reached: --"
    const val UNKNOWN_SOURCE_LABEL: String = "Source: --"
    const val UNKNOWN_SAMPLE_COUNT_LABEL: String = "Weight readings: --"
    const val UNKNOWN_DOSE_LABEL: String = "Dose: --"
    const val UNKNOWN_QUALITY_LABEL: String = "Data status: --"
    private const val CREATED_AT_PATTERN: String = "MMM d, HH:mm:ss"

    fun fromEntity(entity: ShotEntity): ShotHistoryItem =
        summaryFromJson(entity.json).let { summary ->
            ShotHistoryItem(
                id = entity.id,
                createdAtEpochMillis = entity.createdAtEpochMillis,
                createdLabel = createdLabel(entity.createdAtEpochMillis),
                comparisonTitleLabel = comparisonTitleLabel(entity),
                comparisonMetadataLabel = comparisonMetadataLabel(entity),
                comparisonMetricsLabel = comparisonMetricsLabel(summary),
                sourceLabel = summary.sourceLabel,
                qualityLabel = summary.qualityLabel,
                finalYieldLabel = summary.finalYieldLabel,
                flowTimeLabel = summary.flowTimeLabel,
                averageFlowLabel = summary.averageFlowLabel,
                sampleCountLabel = summary.sampleCountLabel,
                doseLabel = summary.doseLabel,
                targetYieldLabel = summary.targetYieldLabel,
                ratioLabel = summary.ratioLabel
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
        val targetRatio = target?.doubleAt("targetRatio")
        val averageFlowGPerS = result?.doubleAt("averageFlowGPerS")
            ?: actualYieldG?.let { yield ->
                flowTimeMs?.takeIf { timeMs -> timeMs > 0L }?.let { timeMs ->
                    yield / (timeMs / 1_000.0)
                }
            }
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
            sampleCount == 0L -> "Data status: No readings"
            flowTimeMs == 0L -> "Data status: Zero flow time"
            target == null || doseG == null || targetYieldG == null -> "Data status: Missing target"
            sampleCount != null && flowTimeMs != null -> "Data status: Complete"
            else -> UNKNOWN_QUALITY_LABEL
        }

        return ShotHistorySummary(
            sourceLabel = sourceLabel,
            qualityLabel = qualityLabel,
            finalYieldLabel = actualYieldG?.let { yield -> "Yield: ${yield.toOneDecimal()} g" }
                ?: UNKNOWN_YIELD_LABEL,
            flowTimeLabel = flowTimeMs
                ?.takeIf { timeMs -> timeMs >= 0L }
                ?.let { timeMs -> "Flow time: ${timeMs.toFlowTimeText()} s" }
                ?: UNKNOWN_FLOW_TIME_LABEL,
            sampleCountLabel = sampleCount?.let { count -> "Weight readings: $count" }
                ?: UNKNOWN_SAMPLE_COUNT_LABEL,
            doseLabel = doseG?.let { dose -> "Dose: ${dose.toOneDecimal()} g" }
                ?: UNKNOWN_DOSE_LABEL,
            targetYieldLabel = targetYieldG?.let { target -> "Target: ${target.toOneDecimal()} g" }
                ?: UNKNOWN_TARGET_YIELD_LABEL,
            ratioLabel = targetRatio?.let { ratio -> "Ratio: 1:${ratio.toRatioText()}" }
                ?: UNKNOWN_RATIO_LABEL,
            averageFlowLabel = averageFlowGPerS?.let { flow -> "Average flow: ${flow.toOneDecimal()} g/s" }
                ?: UNKNOWN_AVERAGE_FLOW_LABEL,
            targetReachedLabel = targetReachedLabel,
            actualYieldG = actualYieldG,
            flowTimeMs = flowTimeMs,
            averageFlowGPerS = averageFlowGPerS,
            doseG = doseG
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
        val ratioLabel: String = UNKNOWN_RATIO_LABEL,
        val averageFlowLabel: String = UNKNOWN_AVERAGE_FLOW_LABEL,
        val targetReachedLabel: String = UNKNOWN_TARGET_REACHED_LABEL,
        val actualYieldG: Double? = null,
        val flowTimeMs: Long? = null,
        val averageFlowGPerS: Double? = null,
        val doseG: Double? = null
    )

    private fun comparisonTitleLabel(entity: ShotEntity): String {
        val beanName = entity.beanName?.trim().orEmpty().ifBlank { UNASSIGNED_BEAN_LABEL }
        val rating = entity.rating
            ?.takeIf { value -> value in 1..5 }
            ?.let { value -> "Rating $value/5" }
        return listOfNotNull(beanName, rating).joinToString(" | ")
    }

    private fun comparisonMetadataLabel(entity: ShotEntity): String? {
        val grind = entity.grindSetting?.trim().orEmpty()
            .ifBlank { null }
            ?.let { value -> "Grind $value" }
        val taste = entity.tasteDirection?.trim().orEmpty()
            .ifBlank { null }
            ?.let(::tasteDirectionLabel)
        return listOfNotNull(grind, taste)
            .takeIf { parts -> parts.isNotEmpty() }
            ?.joinToString(" | ")
    }

    private fun comparisonMetricsLabel(summary: ShotHistorySummary): String {
        val dose = summary.doseG?.let { value -> "${value.toOneDecimal()} g" }
        val yield = summary.actualYieldG?.let { value -> "${value.toOneDecimal()} g" }
        val doseToYield = when {
            dose != null && yield != null -> "$dose -> $yield"
            yield != null -> yield
            dose != null -> "$dose -> --"
            else -> null
        }
        val flowTime = summary.flowTimeMs
            ?.takeIf { timeMs -> timeMs >= 0L }
            ?.let { timeMs -> "${timeMs.toFlowTimeText()} s" }
        val averageFlow = summary.averageFlowGPerS?.let { value -> "${value.toOneDecimal()} g/s" }

        return listOfNotNull(doseToYield, flowTime, averageFlow)
            .takeIf { parts -> parts.isNotEmpty() }
            ?.joinToString(" | ")
            ?: UNKNOWN_COMPARISON_METRICS_LABEL
    }

    private fun tasteDirectionLabel(value: String): String? =
        when (value) {
            "SOUR" -> "Sour"
            "BALANCED" -> "Balanced"
            "BITTER" -> "Bitter"
            else -> null
        }

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

    fun createdLabel(createdAtEpochMillis: Long): String =
        "Created: ${SimpleDateFormat(CREATED_AT_PATTERN, Locale.getDefault()).format(Date(createdAtEpochMillis))}"

    private fun Double.toOneDecimal(): String =
        ((this * 10.0).roundToInt() / 10.0).toString()

    private fun Long.toFlowTimeText(): String {
        val seconds = this / 1_000.0
        return if (this >= 10_000L && this % 1_000L == 0L) {
            seconds.toLong().toString()
        } else {
            seconds.toOneDecimal()
        }
    }

    private fun Double.toRatioText(): String {
        val rounded = (this * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}
