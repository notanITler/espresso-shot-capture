package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShotHistoryViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeShotDao
    private lateinit var viewModel: ShotHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeShotDao()
        viewModel = ShotHistoryViewModel(
            shotRepository = ShotRepository(dao)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyRepositoryProducesEmptyUiState() = runTest(testDispatcher) {
        assertEquals(
            ShotHistoryUiState(items = emptyList()),
            viewModel.uiState.value
        )
    }

    @Test
    fun repositoryEmissionsShowNewestShotsFirstAndMapIdsAndCreatedAtEpochMillis() = runTest(testDispatcher) {
        dao.insertShot(shotEntity(id = "shot-3", createdAtEpochMillis = 3_000L))
        dao.insertShot(shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L))
        dao.insertShot(shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L))

        val uiState = viewModel.uiState
            .first { state -> state.items.size == 3 }

        assertEquals(
            ShotHistoryUiState(
                items = listOf(
                    ShotHistoryItem(
                        id = "shot-3",
                        createdAtEpochMillis = 3_000L,
                        qualityLabel = "Data status: Missing target"
                    ),
                    ShotHistoryItem(
                        id = "shot-2",
                        createdAtEpochMillis = 2_000L,
                        qualityLabel = "Data status: Missing target"
                    ),
                    ShotHistoryItem(
                        id = "shot-1",
                        createdAtEpochMillis = 1_000L,
                        qualityLabel = "Data status: Missing target"
                    )
                )
            ),
            uiState
        )
    }

    @Test
    fun selectingShotShowsDetailFromRepositoryJson() = runTest(testDispatcher) {
        val json = """{"schemaVersion":1,"shot":{"id":"shot-2","status":"COMPLETED"}}"""
        dao.insertShot(shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L))
        dao.insertShot(
            shotEntity(
                id = "shot-2",
                createdAtEpochMillis = 2_000L,
                json = json
            )
        )

        viewModel.selectShot("shot-2")

        val uiState = viewModel.uiState
            .first { state -> state.selectedShotDetail?.id == "shot-2" }

        assertEquals(
            ShotHistoryDetail(
                id = "shot-2",
                createdAtEpochMillis = 2_000L,
                json = json,
                qualityLabel = "Data status: Missing target"
            ),
            uiState.selectedShotDetail
        )
    }

    private fun shotEntity(
        id: String,
        createdAtEpochMillis: Long,
        json: String = """{"schemaVersion":1,"shot":{"id":"$id"}}"""
    ): ShotEntity =
        ShotEntity(
            id = id,
            json = json,
            createdAtEpochMillis = createdAtEpochMillis
        )
}

private class FakeShotDao : ShotDao {
    private val shots = mutableListOf<ShotEntity>()
    private val shotsFlow = MutableStateFlow<List<ShotEntity>>(emptyList())

    override fun insertShot(entity: ShotEntity) {
        shots.removeAll { shot -> shot.id == entity.id }
        shots.add(entity)
        shotsFlow.value = orderedShots()
    }

    override fun observeShots(): Flow<List<ShotEntity>> = shotsFlow

    override fun getShotById(id: String): ShotEntity? =
        shots.firstOrNull { shot -> shot.id == id }

    override fun getAllShotsOnce(): List<ShotEntity> =
        orderedShots()

    override fun deleteShotById(id: String) {
        shots.removeAll { shot -> shot.id == id }
        shotsFlow.value = orderedShots()
    }

    private fun orderedShots(): List<ShotEntity> =
        shots.sortedByDescending { shot -> shot.createdAtEpochMillis }
}
