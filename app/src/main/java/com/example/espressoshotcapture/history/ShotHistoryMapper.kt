package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.roundToInt

object ShotHistoryMapper {
    const val UNKNOWN_YIELD_LABEL: String = "Yield: --"
    const val UNKNOWN_FLOW_TIME_LABEL: String = "Flow time: --"
    const val UNKNOWN_TARGET_YIELD_LABEL: String = "Target: --"

    fun fromEntity(entity: ShotEntity): ShotHistoryItem =
        summaryFromJson(entity.json).let { summary ->
            ShotHistoryItem(
                id = entity.id,
                createdAtEpochMillis = entity.createdAtEpochMillis,
                finalYieldLabel = summary.finalYieldLabel,
                flowTimeLabel = summary.flowTimeLabel,
                targetYieldLabel = summary.targetYieldLabel
            )
        }

    fun fromEntities(entities: List<ShotEntity>): List<ShotHistoryItem> =
        entities.map(::fromEntity)

    private fun summaryFromJson(json: String): ShotHistorySummary {
        val shot = runCatching {
            Json.parseToJsonElement(json).jsonObject["shot"]?.jsonObject
        }.getOrNull() ?: return ShotHistorySummary()

        val actualYieldG = shot.objectAt("result")?.doubleAt("actualYieldG")
            ?: shot.arrayAt("samples")
                ?.lastOrNull()
                ?.let { sample -> sample as? JsonObject }
                ?.doubleAt("weightGRaw")
        val flowTimeMs = shot.objectAt("timing")?.longAt("flowTimeMs")
        val targetYieldG = shot.objectAt("target")?.doubleAt("targetYieldG")

        return ShotHistorySummary(
            finalYieldLabel = actualYieldG?.let { yield -> "Yield: ${yield.toOneDecimal()} g" }
                ?: UNKNOWN_YIELD_LABEL,
            flowTimeLabel = flowTimeMs?.let { timeMs -> "Flow time: ${timeMs / 1_000L} s" }
                ?: UNKNOWN_FLOW_TIME_LABEL,
            targetYieldLabel = targetYieldG?.let { target -> "Target: ${target.toOneDecimal()} g" }
                ?: UNKNOWN_TARGET_YIELD_LABEL
        )
    }

    private data class ShotHistorySummary(
        val finalYieldLabel: String = UNKNOWN_YIELD_LABEL,
        val flowTimeLabel: String = UNKNOWN_FLOW_TIME_LABEL,
        val targetYieldLabel: String = UNKNOWN_TARGET_YIELD_LABEL
    )

    private fun JsonObject.objectAt(key: String): JsonObject? =
        this[key] as? JsonObject

    private fun JsonObject.arrayAt(key: String): JsonArray? =
        this[key] as? JsonArray

    private fun JsonObject.doubleAt(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.longAt(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun Double.toOneDecimal(): String =
        ((this * 10.0).roundToInt() / 10.0).toString()
}
