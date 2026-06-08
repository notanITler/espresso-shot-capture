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
                    "target": { "targetYieldG": 36.0 },
                    "timing": { "flowTimeMs": 28000 },
                    "result": { "actualYieldG": 36.8 },
                    "samples": []
                  }
                }
            """.trimIndent(),
            createdAtEpochMillis = 1_000L
        )

        assertEquals(
            ShotHistoryItem(
                id = "shot-1",
                createdAtEpochMillis = 1_000L,
                finalYieldLabel = "Yield: 36.8 g",
                flowTimeLabel = "Flow time: 28 s",
                targetYieldLabel = "Target: 36.0 g"
            ),
            ShotHistoryMapper.fromEntity(entity)
        )
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
