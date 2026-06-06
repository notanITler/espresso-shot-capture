package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ShotHistoryViewModelTest {
    @Test
    fun emptyInputProducesEmptyUiState() {
        val viewModel = ShotHistoryViewModel()

        assertEquals(
            ShotHistoryUiState(items = emptyList()),
            viewModel.uiState.value
        )
    }

    @Test
    fun populatedInputPreservesOrderAndMapsIdsAndCreatedAtEpochMillis() {
        val viewModel = ShotHistoryViewModel(
            initialShots = listOf(
                shotEntity(id = "shot-3", createdAtEpochMillis = 3_000L),
                shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L),
                shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L)
            )
        )

        assertEquals(
            ShotHistoryUiState(
                items = listOf(
                    ShotHistoryItem(id = "shot-3", createdAtEpochMillis = 3_000L),
                    ShotHistoryItem(id = "shot-1", createdAtEpochMillis = 1_000L),
                    ShotHistoryItem(id = "shot-2", createdAtEpochMillis = 2_000L)
                )
            ),
            viewModel.uiState.value
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
