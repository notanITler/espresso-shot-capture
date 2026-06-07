package com.example.espressoshotcapture.capture

import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeShotDao
    private lateinit var viewModel: CaptureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeShotDao()
        viewModel = CaptureViewModel(
            shotRepository = ShotRepository(dao),
            saveDispatcher = testDispatcher,
            currentTimeMillis = { 123_456L },
            savedConfirmationDelayMs = 1_000L
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateComesFromInitialDisconnectedReadyMapper() = runTest(testDispatcher) {
        assertEquals(
            CaptureUiStateMapper.initialDisconnectedReady(),
            viewModel.uiState.value
        )
    }

    @Test
    fun startCaptureMovesReadyToRecording() = runTest(testDispatcher) {
        viewModel.onPrimaryAction()

        assertEquals(
            CaptureUiStateMapper.recording(),
            viewModel.uiState.value
        )
    }

    @Test
    fun stopAndSaveSavesShotDraftThroughRepository() = runTest(testDispatcher) {
        viewModel.onPrimaryAction()
        viewModel.onPrimaryAction()
        runCurrent()

        val savedShot = dao.getAllShotsOnce().single()
        assertEquals("fake-shot-123456", savedShot.id)
        assertEquals(123_456L, savedShot.createdAtEpochMillis)
        assertTrue(savedShot.json.contains(""""schemaVersion":1"""))
        assertTrue(savedShot.json.contains(""""id":"fake-shot-123456""""))
        assertTrue(savedShot.json.contains(""""actualYieldG":36.8"""))
    }

    @Test
    fun afterSaveConfirmationStateReturnsToReady() = runTest(testDispatcher) {
        viewModel.onPrimaryAction()
        viewModel.onPrimaryAction()
        runCurrent()

        assertEquals(
            CaptureUiStateMapper.savedConfirmation(),
            viewModel.uiState.value
        )

        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(
            CaptureUiStateMapper.initialDisconnectedReady(),
            viewModel.uiState.value
        )
    }
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
