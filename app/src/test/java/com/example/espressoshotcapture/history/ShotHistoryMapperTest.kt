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
                    "target": { "doseG": 18.0, "targetYieldG": 36.0 },
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
                sourceLabel = "Source: Fake/demo",
                qualityLabel = "Quality: Complete",
                finalYieldLabel = "Yield: 36.8 g",
                flowTimeLabel = "Flow time: 28 s",
                sampleCountLabel = "Samples: 3",
                doseLabel = "Dose: 18.0 g",
                targetYieldLabel = "Target: 36.0 g"
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
                    "target": { "doseG": 18.0, "targetYieldG": 36.0 },
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
        assertEquals("Quality: Complete", summary.qualityLabel)
        assertEquals("Yield: 37.2 g", summary.finalYieldLabel)
        assertEquals("Flow time: 28 s", summary.flowTimeLabel)
        assertEquals("Average flow: 1.3 g/s", summary.averageFlowLabel)
        assertEquals("Samples: 4", summary.sampleCountLabel)
        assertEquals("Dose: 18.0 g", summary.doseLabel)
        assertEquals("Target: 36.0 g", summary.targetYieldLabel)
        assertEquals("Target reached: yes", summary.targetReachedLabel)
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

        assertEquals("Quality: No samples", summary.qualityLabel)
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

        assertEquals("Quality: Zero flow time", summary.qualityLabel)
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

        assertEquals("Quality: Missing target", summary.qualityLabel)
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
                finalYieldLabel = "Yield: --",
                flowTimeLabel = "Flow time: --",
                qualityLabel = "Quality: Missing target",
                targetYieldLabel = "Target: --"
            ),
            ShotHistoryMapper.fromEntity(entity)
        )
    }

    private fun shotEntity(
        id: String,
        json: String = """{"schemaVersion":1}""",
        createdAtEpochMillis: Long
    ): ShotEntity =
        ShotEntity(
            id = id,
            json = json,
            createdAtEpochMillis = createdAtEpochMillis
        )
}
