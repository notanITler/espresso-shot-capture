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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            shotRepository = ShotRepository(dao),
            saveDispatcher = testDispatcher
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
    fun repositoryEmissionsPreserveOrderAndMapIdsAndCreatedAtEpochMillis() = runTest(testDispatcher) {
        dao.insertShot(shotEntity(id = "shot-3", createdAtEpochMillis = 3_000L))
        dao.insertShot(shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L))
        dao.insertShot(shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L))

        val uiState = viewModel.uiState
            .first { state -> state.items.size == 3 }

        assertEquals(
            ShotHistoryUiState(
                items = listOf(
                    ShotHistoryItem(id = "shot-3", createdAtEpochMillis = 3_000L),
                    ShotHistoryItem(id = "shot-1", createdAtEpochMillis = 1_000L),
                    ShotHistoryItem(id = "shot-2", createdAtEpochMillis = 2_000L)
                )
            ),
            uiState
        )
    }

    @Test
    fun addTestShotSavesDraftThroughRepository() = runTest(testDispatcher) {
        viewModel.addTestShot()
        advanceUntilIdle()

        val savedShot = dao.getAllShotsOnce().single()
        assertTrue(savedShot.id.startsWith("test-shot-"))
        assertTrue(savedShot.json.contains(""""schemaVersion":1"""))
        assertTrue(savedShot.json.contains(""""id":"${savedShot.id}""""))
        assertTrue(savedShot.json.contains(""""target":{"""))
        assertTrue(savedShot.json.contains(""""timing":{"""))
        assertTrue(savedShot.json.contains(""""result":{"""))
        assertTrue(savedShot.json.contains(""""doseG":18.0"""))
        assertTrue(savedShot.json.contains(""""sampleCount":0"""))
        assertTrue(savedShot.createdAtEpochMillis > 0L)
    }

    @Test
    fun addTestShotUpdatesHistoryUiState() = runTest(testDispatcher) {
        viewModel.addTestShot()
        advanceUntilIdle()

        val savedShot = dao.getAllShotsOnce().single()
        val uiState = viewModel.uiState
            .first { state -> state.items.isNotEmpty() }

        assertEquals(
            ShotHistoryUiState(
                items = listOf(
                    ShotHistoryItem(
                        id = savedShot.id,
                        createdAtEpochMillis = savedShot.createdAtEpochMillis
                    )
                )
            ),
            uiState
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

private class FakeShotDao : ShotDao {
    private val shots = mutableListOf<ShotEntity>()
    private val shotsFlow = MutableStateFlow<List<ShotEntity>>(emptyList())

    override fun insertShot(entity: ShotEntity) {
        shots.removeAll { shot -> shot.id == entity.id }
        shots.add(entity)
        shotsFlow.value = shots.toList()
    }

    override fun observeShots(): Flow<List<ShotEntity>> = shotsFlow

    override fun getShotById(id: String): ShotEntity? =
        shots.firstOrNull { shot -> shot.id == id }

    override fun getAllShotsOnce(): List<ShotEntity> =
        shots.toList()

    override fun deleteShotById(id: String) {
        shots.removeAll { shot -> shot.id == id }
        shotsFlow.value = shots.toList()
    }
}
