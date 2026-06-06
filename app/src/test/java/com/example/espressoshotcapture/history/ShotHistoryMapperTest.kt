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
    fun doesNotRequireJsonParsing() {
        val entity = shotEntity(
            id = "shot-1",
            json = "not-json",
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
