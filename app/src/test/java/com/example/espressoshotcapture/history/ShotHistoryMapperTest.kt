package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ShotHistoryMapperTest {
    @Test
    fun mapsEntityToHistoryItem() {
        val entity = shotEntity(
            id = "shot-1",
            createdAtEpochMillis = 1_000L
        )

        assertEquals(
            ShotHistoryItem(
                id = "shot-1",
                createdAtEpochMillis = 1_000L
            ),
            ShotHistoryMapper.fromEntity(entity)
        )
    }

    @Test
    fun extractsSummaryFromValidShotJson() {
        val entity = shotEntity(
            id = "shot-1",
            json = """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "metadata": { "scaleSource": "FAKE_DEMO" },
                    "target": { "doseG": 18.0, "targetYieldG": 36.0, "targetRatio": 2.0 },
                    "timing": { "flowTimeMs": 28000 },
                    "result": { "actualYieldG": 36.8, "sampleCount": 3 },
                    "samples": [{}, {}, {}]
                  }
                }
            """.trimIndent(),
            createdAtEpochMillis = 1_000L
        )

        assertEquals(
            ShotHistoryItem(
                id = "shot-1",
                createdAtEpochMillis = 1_000L,
                comparisonMetricsLabel = "18.0 g -> 36.8 g | 28 s | 1.3 g/s",
                sourceLabel = "Source: Fake/demo",
                qualityLabel = "Data status: Complete",
                finalYieldLabel = "Yield: 36.8 g",
                flowTimeLabel = "Flow time: 28 s",
                averageFlowLabel = "Average flow: 1.3 g/s",
                sampleCountLabel = "Weight readings: 3",
                doseLabel = "Dose: 18.0 g",
                targetYieldLabel = "Target: 36.0 g",
                ratioLabel = "Ratio: 1:2"
            ),
            ShotHistoryMapper.fromEntity(entity)
        )
    }

    @Test
    fun extractsExtendedSummaryFromValidShotJson() {
        val summary = ShotHistoryMapper.summaryFromJson(
            """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "metadata": { "scaleSource": "DECENT_SCALE" },
                    "target": { "doseG": 18.0, "targetYieldG": 36.0, "targetRatio": 2.0 },
                    "timing": {
                      "flowTimeMs": 28000,
                      "targetReachedAtMs": 24000
                    },
                    "result": {
                      "actualYieldG": 37.2,
                      "averageFlowGPerS": 1.28,
                      "sampleCount": 4
                    },
                    "samples": [{}, {}, {}, {}]
                  }
                }
            """.trimIndent()
        )

        assertEquals("Source: Decent Scale", summary.sourceLabel)
        assertEquals("Data status: Complete", summary.qualityLabel)
        assertEquals("Yield: 37.2 g", summary.finalYieldLabel)
        assertEquals("Flow time: 28 s", summary.flowTimeLabel)
        assertEquals("Average flow: 1.3 g/s", summary.averageFlowLabel)
        assertEquals("Weight readings: 4", summary.sampleCountLabel)
        assertEquals("Dose: 18.0 g", summary.doseLabel)
        assertEquals("Target: 36.0 g", summary.targetYieldLabel)
        assertEquals("Ratio: 1:2", summary.ratioLabel)
        assertEquals("Target reached: yes", summary.targetReachedLabel)
    }

    @Test
    fun derivesAverageFlowWhenStoredAverageIsMissing() {
        val summary = ShotHistoryMapper.summaryFromJson(
            """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "timing": { "flowTimeMs": 2845 },
                    "result": { "actualYieldG": 40.4, "sampleCount": 4 }
                  }
                }
            """.trimIndent()
        )

        assertEquals("Flow time: 2.8 s", summary.flowTimeLabel)
        assertEquals("Average flow: 14.2 g/s", summary.averageFlowLabel)
    }

    @Test
    fun averageFlowStaysUnknownWhenItCannotBeComputedSafely() {
        val summary = ShotHistoryMapper.summaryFromJson(
            """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "timing": { "flowTimeMs": 0 },
                    "result": { "actualYieldG": 42.0, "sampleCount": 4 }
                  }
                }
            """.trimIndent()
        )

        assertEquals("Flow time: 0.0 s", summary.flowTimeLabel)
        assertEquals("Average flow: --", summary.averageFlowLabel)
    }

    @Test
    fun impossibleFlowDurationUsesUnknownMetrics() {
        val summary = ShotHistoryMapper.summaryFromJson(
            """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "timing": { "flowTimeMs": -1 },
                    "result": { "actualYieldG": 40.4, "sampleCount": 4 }
                  }
                }
            """.trimIndent()
        )

        assertEquals("Flow time: --", summary.flowTimeLabel)
        assertEquals("Average flow: --", summary.averageFlowLabel)
    }

    @Test
    fun noSamplesProducesQualityLabel() {
        val summary = ShotHistoryMapper.summaryFromJson(
            """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "target": { "doseG": 18.0, "targetYieldG": 36.0 },
                    "timing": { "flowTimeMs": 28000 },
                    "result": { "sampleCount": 0 },
                    "samples": []
                  }
                }
            """.trimIndent()
        )

        assertEquals("Data status: No readings", summary.qualityLabel)
    }

    @Test
    fun zeroFlowTimeProducesQualityLabel() {
        val summary = ShotHistoryMapper.summaryFromJson(
            """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "target": { "doseG": 18.0, "targetYieldG": 36.0 },
                    "timing": { "flowTimeMs": 0 },
                    "result": { "sampleCount": 2 },
                    "samples": [{}, {}]
                  }
                }
            """.trimIndent()
        )

        assertEquals("Data status: Zero flow time", summary.qualityLabel)
    }

    @Test
    fun missingTargetProducesQualityLabel() {
        val summary = ShotHistoryMapper.summaryFromJson(
            """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "timing": { "flowTimeMs": 28000 },
                    "result": { "sampleCount": 2 },
                    "samples": [{}, {}]
                  }
                }
            """.trimIndent()
        )

        assertEquals("Data status: Missing target", summary.qualityLabel)
    }

    @Test
    fun nullTargetReachedIsShownAsNotReached() {
        val summary = ShotHistoryMapper.summaryFromJson(
            """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "timing": { "targetReachedAtMs": null }
                  }
                }
            """.trimIndent()
        )

        assertEquals("Target reached: no", summary.targetReachedLabel)
    }

    @Test
    fun usesLastSampleWeightWhenActualYieldIsMissing() {
        val entity = shotEntity(
            id = "shot-1",
            json = """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "target": { "targetYieldG": 36.0 },
                    "timing": { "flowTimeMs": 25000 },
                    "result": {},
                    "samples": [
                      { "weightGRaw": 12.2 },
                      { "weightGRaw": 35.9 }
                    ]
                  }
                }
            """.trimIndent(),
            createdAtEpochMillis = 1_000L
        )

        assertEquals("Yield: 35.9 g", ShotHistoryMapper.fromEntity(entity).finalYieldLabel)
        assertEquals("35.9 g | 25 s | 1.4 g/s", ShotHistoryMapper.fromEntity(entity).comparisonMetricsLabel)
    }

    @Test
    fun historyItemUsesMetadataFirstComparisonLabelsWhenAvailable() {
        val entity = shotEntity(
            id = "shot-metadata",
            json = """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "target": { "doseG": 18.0, "targetYieldG": 36.0 },
                    "timing": { "flowTimeMs": 27800 },
                    "result": { "actualYieldG": 36.5, "averageFlowGPerS": 1.31, "sampleCount": 10 }
                  }
                }
            """.trimIndent(),
            createdAtEpochMillis = 1_000L,
            rating = 4,
            tasteDirection = "BALANCED",
            grindSetting = "8.10",
            beanName = "Delta Espresso Bar"
        )

        val item = ShotHistoryMapper.fromEntity(entity)

        assertEquals("Delta Espresso Bar | Rating 4/5", item.comparisonTitleLabel)
        assertEquals("Grind 8.10 | Balanced", item.comparisonMetadataLabel)
        assertEquals("18.0 g -> 36.5 g | 27.8 s | 1.3 g/s", item.comparisonMetricsLabel)
    }

    @Test
    fun historyItemUsesObjectiveMetricsWhenMetadataIsMissing() {
        val entity = shotEntity(
            id = "shot-objective",
            json = """
                {
                  "schemaVersion": 1,
                  "shot": {
                    "target": { "doseG": 18.0, "targetYieldG": 36.0 },
                    "timing": { "flowTimeMs": 27800 },
                    "result": { "actualYieldG": 36.5, "averageFlowGPerS": 1.31, "sampleCount": 10 }
                  }
                }
            """.trimIndent(),
            createdAtEpochMillis = 1_000L
        )

        val item = ShotHistoryMapper.fromEntity(entity)

        assertEquals("Unassigned bean", item.comparisonTitleLabel)
        assertEquals(null, item.comparisonMetadataLabel)
        assertEquals("18.0 g -> 36.5 g | 27.8 s | 1.3 g/s", item.comparisonMetricsLabel)
    }

    @Test
    fun mapsEntitiesToHistoryItems() {
        val entities = listOf(
            shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L),
            shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L)
        )

        assertEquals(
            listOf(
                ShotHistoryItem(id = "shot-1", createdAtEpochMillis = 1_000L),
                ShotHistoryItem(id = "shot-2", createdAtEpochMillis = 2_000L)
            ),
            ShotHistoryMapper.fromEntities(entities)
        )
    }

    @Test
    fun preservesEntityOrder() {
        val entities = listOf(
            shotEntity(id = "shot-3", createdAtEpochMillis = 3_000L),
            shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L),
            shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L)
        )

        assertEquals(
            listOf("shot-3", "shot-1", "shot-2"),
            ShotHistoryMapper.fromEntities(entities).map { it.id }
        )
    }

    @Test
    fun invalidJsonUsesFallbackSummaryLabels() {
        val entity = shotEntity(
            id = "shot-1",
            json = "not-json",
            createdAtEpochMillis = 1_000L
        )

        assertEquals(
            ShotHistoryItem(
                id = "shot-1",
                createdAtEpochMillis = 1_000L,
                comparisonMetricsLabel = "--",
                finalYieldLabel = "Yield: --",
                flowTimeLabel = "Flow time: --",
                targetYieldLabel = "Target: --"
            ),
            ShotHistoryMapper.fromEntity(entity)
        )
    }

    @Test
    fun missingJsonFieldsUseFallbackSummaryLabels() {
        val entity = shotEntity(
            id = "shot-1",
            json = """{"schemaVersion":1,"shot":{}}""",
            createdAtEpochMillis = 1_000L
        )

        assertEquals(
            ShotHistoryItem(
                id = "shot-1",
                createdAtEpochMillis = 1_000L,
                comparisonMetricsLabel = "--",
                finalYieldLabel = "Yield: --",
                flowTimeLabel = "Flow time: --",
                qualityLabel = "Data status: Missing target",
                targetYieldLabel = "Target: --"
            ),
            ShotHistoryMapper.fromEntity(entity)
        )
    }

    private fun shotEntity(
        id: String,
        json: String = """{"schemaVersion":1}""",
        createdAtEpochMillis: Long,
        rating: Int? = null,
        tasteDirection: String? = null,
        grindSetting: String? = null,
        beanName: String? = null
    ): ShotEntity =
        ShotEntity(
            id = id,
            json = json,
            createdAtEpochMillis = createdAtEpochMillis,
            rating = rating,
            tasteDirection = tasteDirection,
            grindSetting = grindSetting,
            beanName = beanName
        )
}
