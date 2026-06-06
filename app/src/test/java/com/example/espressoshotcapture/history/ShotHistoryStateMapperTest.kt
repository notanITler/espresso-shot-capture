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
                    ShotHistoryItem(id = "shot-1", createdAtEpochMillis = 1_000L),
                    ShotHistoryItem(id = "shot-2", createdAtEpochMillis = 2_000L)
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

    private fun shotEntity(
        id: String,
        createdAtEpochMillis: Long
    ): ShotEntity =
        ShotEntity(
            id = id,
            json = """{"schemaVersion":1,"shot":{"id":"$id"}}""",
            createdAtEpochMillis = createdAtEpochMillis
        )
}
