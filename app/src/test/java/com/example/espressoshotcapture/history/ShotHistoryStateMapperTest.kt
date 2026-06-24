package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ShotHistoryStateMapperTest {
    @Test
    fun mapsEntitiesToUiStateItems() {
        val entities = listOf(
            shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L),
            shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L)
        )

        assertEquals(
            ShotHistoryUiState(
                items = listOf(
                    ShotHistoryItem(
                        id = "shot-1",
                        createdAtEpochMillis = 1_000L,
                        qualityLabel = "Data status: Missing target"
                    ),
                    ShotHistoryItem(
                        id = "shot-2",
                        createdAtEpochMillis = 2_000L,
                        qualityLabel = "Data status: Missing target"
                    )
                ),
                beanFilterOptions = listOf(
                    ShotHistoryBeanFilterOption(
                        key = ShotHistoryBeanFilterKeys.ALL,
                        label = "All shots",
                        isSelected = true
                    ),
                    ShotHistoryBeanFilterOption(
                        key = ShotHistoryBeanFilterKeys.UNASSIGNED,
                        label = "Unassigned",
                        isSelected = false
                    )
                )
            ),
            ShotHistoryStateMapper.fromEntities(entities)
        )
    }

    @Test
    fun emptyEntitiesReturnEmptyUiState() {
        assertEquals(
            ShotHistoryUiState(items = emptyList()),
            ShotHistoryStateMapper.fromEntities(emptyList())
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
            ShotHistoryStateMapper.fromEntities(entities).items.map { it.id }
        )
    }

    @Test
    fun beanFilterShowsOnlyMatchingBeanShots() {
        val entities = listOf(
            shotEntity(id = "shot-delta", createdAtEpochMillis = 3_000L, beanName = "Delta"),
            shotEntity(id = "shot-hannover", createdAtEpochMillis = 2_000L, beanName = "Hannover"),
            shotEntity(id = "shot-empty", createdAtEpochMillis = 1_000L)
        )

        val uiState = ShotHistoryStateMapper.fromEntities(
            entities = entities,
            selectedBeanFilterKey = ShotHistoryBeanFilterKeys.bean("delta")
        )

        assertEquals(listOf("shot-delta"), uiState.items.map { item -> item.id })
        assertEquals(
            listOf(
                ShotHistoryBeanFilterOption("all", "All shots", false),
                ShotHistoryBeanFilterOption("unassigned", "Unassigned", false),
                ShotHistoryBeanFilterOption("bean:delta", "Delta", true),
                ShotHistoryBeanFilterOption("bean:hannover", "Hannover", false)
            ),
            uiState.beanFilterOptions
        )
    }

    @Test
    fun unassignedFilterShowsShotsWithoutBeanName() {
        val entities = listOf(
            shotEntity(id = "shot-delta", createdAtEpochMillis = 3_000L, beanName = "Delta"),
            shotEntity(id = "shot-empty", createdAtEpochMillis = 2_000L),
            shotEntity(id = "shot-blank", createdAtEpochMillis = 1_000L, beanName = "   ")
        )

        val uiState = ShotHistoryStateMapper.fromEntities(
            entities = entities,
            selectedBeanFilterKey = ShotHistoryBeanFilterKeys.UNASSIGNED
        )

        assertEquals(listOf("shot-empty", "shot-blank"), uiState.items.map { item -> item.id })
    }

    @Test
    fun caseAndWhitespaceBeanNamesShareOneFilterOption() {
        val entities = listOf(
            shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L, beanName = "Delta"),
            shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L, beanName = " delta ")
        )

        val uiState = ShotHistoryStateMapper.fromEntities(entities)

        assertEquals(
            listOf(
                ShotHistoryBeanFilterOption("all", "All shots", true),
                ShotHistoryBeanFilterOption("bean:delta", "Delta", false)
            ),
            uiState.beanFilterOptions
        )
    }

    @Test
    fun selectedShotDetailIsClearedWhenFilteredOut() {
        val entities = listOf(
            shotEntity(id = "shot-delta", createdAtEpochMillis = 1_000L, beanName = "Delta"),
            shotEntity(id = "shot-hannover", createdAtEpochMillis = 2_000L, beanName = "Hannover")
        )

        val uiState = ShotHistoryStateMapper.fromEntities(
            entities = entities,
            selectedShotId = "shot-hannover",
            selectedBeanFilterKey = ShotHistoryBeanFilterKeys.bean("delta")
        )

        assertEquals(null, uiState.selectedShotDetail)
        assertEquals(null, uiState.metadataEditor)
    }

    @Test
    fun selectedShotDetailContainsEntityJson() {
        val json = """{"schemaVersion":1,"shot":{"id":"shot-2"}}"""
        val entities = listOf(
            shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L),
            shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L, json = json)
        )

        assertEquals(
            ShotHistoryDetail(
                id = "shot-2",
                createdAtEpochMillis = 2_000L,
                json = json,
                qualityLabel = "Data status: Missing target"
            ),
            ShotHistoryStateMapper.fromEntities(
                entities = entities,
                selectedShotId = "shot-2"
            ).selectedShotDetail
        )
    }

    @Test
    fun selectedShotDetailContainsPreparedDisplayFields() {
        val json = """
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
        val entities = listOf(
            shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L, json = json)
        )

        assertEquals(
            ShotHistoryDetail(
                id = "shot-1",
                createdAtEpochMillis = 1_000L,
                json = json,
                sourceLabel = "Source: Decent Scale",
                qualityLabel = "Data status: Complete",
                finalYieldLabel = "Yield: 37.2 g",
                flowTimeLabel = "Flow time: 28 s",
                sampleCountLabel = "Weight readings: 4",
                doseLabel = "Dose: 18.0 g",
                targetYieldLabel = "Target: 36.0 g",
                ratioLabel = "Ratio: 1:2",
                averageFlowLabel = "Average flow: 1.3 g/s",
                targetReachedLabel = "Target reached: yes"
            ),
            ShotHistoryStateMapper.fromEntities(
                entities = entities,
                selectedShotId = "shot-1"
            ).selectedShotDetail
        )
    }

    @Test
    fun historyAndSelectedDetailUseConsistentMetricFormatting() {
        val json = """
            {
              "schemaVersion": 1,
              "shot": {
                "timing": { "flowTimeMs": 2845 },
                "result": { "actualYieldG": 40.4, "sampleCount": 4 },
                "samples": [{}, {}, {}, {}]
              }
            }
        """.trimIndent()
        val uiState = ShotHistoryStateMapper.fromEntities(
            entities = listOf(
                shotEntity(id = "shot-short", createdAtEpochMillis = 1_000L, json = json)
            ),
            selectedShotId = "shot-short"
        )

        val historyItem = uiState.items.single()
        val detail = requireNotNull(uiState.selectedShotDetail)

        assertEquals("Yield: 40.4 g", historyItem.finalYieldLabel)
        assertEquals("Flow time: 2.8 s", historyItem.flowTimeLabel)
        assertEquals("Average flow: 14.2 g/s", historyItem.averageFlowLabel)
        assertEquals(historyItem.finalYieldLabel, detail.finalYieldLabel)
        assertEquals(historyItem.flowTimeLabel, detail.flowTimeLabel)
        assertEquals(historyItem.averageFlowLabel, detail.averageFlowLabel)
    }

    private fun shotEntity(
        id: String,
        createdAtEpochMillis: Long,
        json: String = """{"schemaVersion":1,"shot":{"id":"$id"}}""",
        beanName: String? = null
    ): ShotEntity =
        ShotEntity(
            id = id,
            json = json,
            createdAtEpochMillis = createdAtEpochMillis,
            beanName = beanName
        )
}
