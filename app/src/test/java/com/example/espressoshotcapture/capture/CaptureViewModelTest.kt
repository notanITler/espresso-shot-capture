package com.example.espressoshotcapture.capture

import com.example.espressoshotcapture.capture.domain.FakeScaleClient
import com.example.espressoshotcapture.capture.domain.ScaleClient
import com.example.espressoshotcapture.capture.domain.ScaleConnectionState
import com.example.espressoshotcapture.capture.domain.ScaleReading
import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
    private lateinit var scaleClient: TestScaleClient
    private lateinit var viewModel: CaptureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeShotDao()
        scaleClient = TestScaleClient()
        viewModel = CaptureViewModel(
            shotRepository = ShotRepository(dao),
            scaleClient = scaleClient,
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
        scaleClient.emitConnectionState(ScaleConnectionState.Disconnected)
        runCurrent()

        assertEquals(
            CaptureUiStateMapper.initialDisconnectedReady(),
            viewModel.uiState.value
        )
    }

    @Test
    fun initConnectsScaleClientAndShowsConnectedLabel() = runTest(testDispatcher) {
        runCurrent()

        assertEquals("Scale: Connected", viewModel.uiState.value.scaleConnectionLabel)
    }

    @Test
    fun connectingStateShowsConnectingLabel() = runTest(testDispatcher) {
        scaleClient.emitConnectionState(ScaleConnectionState.Connecting)
        runCurrent()

        assertEquals("Scale: Connecting", viewModel.uiState.value.scaleConnectionLabel)
    }

    @Test
    fun connectedStateShowsConnectedLabel() = runTest(testDispatcher) {
        scaleClient.emitConnectionState(ScaleConnectionState.Connected)
        runCurrent()

        assertEquals("Scale: Connected", viewModel.uiState.value.scaleConnectionLabel)
    }

    @Test
    fun disconnectedStateShowsNotConnectedLabel() = runTest(testDispatcher) {
        scaleClient.emitConnectionState(ScaleConnectionState.Disconnected)
        runCurrent()

        assertEquals("Scale: Not connected", viewModel.uiState.value.scaleConnectionLabel)
    }

    @Test
    fun errorStateShowsErrorLabel() = runTest(testDispatcher) {
        scaleClient.emitConnectionState(ScaleConnectionState.Error("Connection failed"))
        runCurrent()

        assertEquals("Scale: Error", viewModel.uiState.value.scaleConnectionLabel)
    }

    @Test
    fun startCaptureMovesReadyToRecording() = runTest(testDispatcher) {
        runCurrent()
        viewModel.onPrimaryAction()

        assertEquals(
            CaptureUiStateMapper.recording(scaleConnectionLabel = "Scale: Connected"),
            viewModel.uiState.value
        )
    }

    @Test
    fun recordingValuesUpdateFromScaleReadings() = runTest(testDispatcher) {
        runCurrent()
        viewModel.onPrimaryAction()
        runCurrent()

        scaleClient.emitReading(
            ScaleReading(timestampMillis = 1_000L, weightGrams = 10.0)
        )
        runCurrent()

        assertEquals("Weight: 10.0 g", viewModel.uiState.value.currentWeightLabel)
        assertEquals("Flow time: 0 s", viewModel.uiState.value.flowTimeLabel)
        assertEquals("Average flow: 0.0 g/s", viewModel.uiState.value.averageFlowLabel)

        scaleClient.emitReading(
            ScaleReading(timestampMillis = 3_000L, weightGrams = 20.0)
        )
        runCurrent()

        assertEquals("Weight: 20.0 g", viewModel.uiState.value.currentWeightLabel)
        assertEquals("Flow time: 2 s", viewModel.uiState.value.flowTimeLabel)
        assertEquals("Average flow: 10.0 g/s", viewModel.uiState.value.averageFlowLabel)
    }

    @Test
    fun readingsAreFedIntoShotCaptureEngineDuringRecording() = runTest(testDispatcher) {
        runCurrent()
        viewModel.onPrimaryAction()
        runCurrent()

        listOf(
            ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
            ScaleReading(timestampMillis = 1_000L, weightGrams = 1.0),
            ScaleReading(timestampMillis = 2_000L, weightGrams = 2.0),
            ScaleReading(timestampMillis = 3_000L, weightGrams = 3.5)
        ).forEach { reading ->
            scaleClient.emitReading(reading)
            runCurrent()
        }

        viewModel.onPrimaryAction()
        runCurrent()

        val savedShot = dao.getAllShotsOnce().single()
        assertEquals("shot-125456", savedShot.id)
        assertTrue(savedShot.json.contains(""""id":"shot-125456""""))
        assertTrue(savedShot.json.contains(""""createdAtEpochMs":125456"""))
        assertTrue(savedShot.json.contains(""""weightGRaw":3.5"""))
        assertTrue(savedShot.json.contains(""""sampleCount":1"""))
        assertTrue(savedShot.json.contains(""""status":"MANUAL_STOPPED""""))
    }

    @Test
    fun fakeRecordingValuesIncreaseDuringOneRecordingSession() = runTest(testDispatcher) {
        val fakeScaleClient = FakeScaleClient(
            readingSequence = listOf(
                ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
                ScaleReading(timestampMillis = 1_000L, weightGrams = 2.0),
                ScaleReading(timestampMillis = 2_000L, weightGrams = 5.0)
            )
        )
        viewModel = CaptureViewModel(
            shotRepository = ShotRepository(dao),
            scaleClient = fakeScaleClient,
            saveDispatcher = testDispatcher,
            currentTimeMillis = { 123_456L },
            savedConfirmationDelayMs = 1_000L
        )
        runCurrent()

        viewModel.onPrimaryAction()
        runCurrent()

        assertEquals("Weight: 0.0 g", viewModel.uiState.value.currentWeightLabel)
        assertEquals("Flow time: 0 s", viewModel.uiState.value.flowTimeLabel)
        assertEquals("Average flow: 0.0 g/s", viewModel.uiState.value.averageFlowLabel)

        advanceTimeBy(500L)
        runCurrent()

        assertEquals("Weight: 2.0 g", viewModel.uiState.value.currentWeightLabel)
        assertEquals("Flow time: 1 s", viewModel.uiState.value.flowTimeLabel)
        assertEquals("Average flow: 2.0 g/s", viewModel.uiState.value.averageFlowLabel)

        advanceTimeBy(500L)
        runCurrent()

        assertEquals("Weight: 5.0 g", viewModel.uiState.value.currentWeightLabel)
        assertEquals("Flow time: 2 s", viewModel.uiState.value.flowTimeLabel)
        assertEquals("Average flow: 2.5 g/s", viewModel.uiState.value.averageFlowLabel)

        viewModel.onPrimaryAction()
        runCurrent()

        assertEquals(CaptureStatus.SAVED, viewModel.uiState.value.status)

        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(CaptureStatus.READY, viewModel.uiState.value.status)
    }

    @Test
    fun stopAndSaveStopsFakeReadingUpdates() = runTest(testDispatcher) {
        val fakeScaleClient = FakeScaleClient(
            readingSequence = listOf(
                ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
                ScaleReading(timestampMillis = 1_000L, weightGrams = 2.0),
                ScaleReading(timestampMillis = 2_000L, weightGrams = 5.0),
                ScaleReading(timestampMillis = 3_000L, weightGrams = 9.0)
            )
        )
        viewModel = CaptureViewModel(
            shotRepository = ShotRepository(dao),
            scaleClient = fakeScaleClient,
            saveDispatcher = testDispatcher,
            currentTimeMillis = { 123_456L },
            savedConfirmationDelayMs = 1_000L
        )
        runCurrent()

        viewModel.onPrimaryAction()
        runCurrent()
        advanceTimeBy(500L)
        runCurrent()

        assertEquals("Weight: 2.0 g", viewModel.uiState.value.currentWeightLabel)
        assertEquals("Flow time: 1 s", viewModel.uiState.value.flowTimeLabel)

        viewModel.onPrimaryAction()
        runCurrent()

        assertEquals(CaptureStatus.SAVED, viewModel.uiState.value.status)
        assertEquals(null, viewModel.uiState.value.currentWeightLabel)
        assertEquals(null, viewModel.uiState.value.flowTimeLabel)
        assertEquals(null, viewModel.uiState.value.averageFlowLabel)

        advanceTimeBy(2_000L)
        runCurrent()

        assertEquals(CaptureStatus.READY, viewModel.uiState.value.status)
        assertEquals(null, viewModel.uiState.value.currentWeightLabel)
        assertEquals(null, viewModel.uiState.value.flowTimeLabel)
        assertEquals(null, viewModel.uiState.value.averageFlowLabel)
    }

    @Test
    fun scaleReadingsDoNotUpdateReadyState() = runTest(testDispatcher) {
        runCurrent()

        scaleClient.emitReading(
            ScaleReading(timestampMillis = 1_000L, weightGrams = 10.0)
        )
        runCurrent()

        assertEquals(null, viewModel.uiState.value.currentWeightLabel)
        assertEquals(null, viewModel.uiState.value.flowTimeLabel)
        assertEquals(null, viewModel.uiState.value.averageFlowLabel)
    }

    @Test
    fun stopAndSaveSavesShotDraftThroughRepository() = runTest(testDispatcher) {
        runCurrent()
        viewModel.onPrimaryAction()
        viewModel.onPrimaryAction()
        runCurrent()

        val savedShot = dao.getAllShotsOnce().single()
        assertEquals("shot-123456", savedShot.id)
        assertEquals(123_456L, savedShot.createdAtEpochMillis)
        assertTrue(savedShot.json.contains(""""schemaVersion":1"""))
        assertTrue(savedShot.json.contains(""""id":"shot-123456""""))
        assertTrue(savedShot.json.contains(""""status":"MANUAL_STOPPED""""))
    }

    @Test
    fun consecutiveCaptureSessionsSaveDistinctShotDrafts() = runTest(testDispatcher) {
        runCurrent()

        saveRecordingSession()
        advanceTimeBy(1_000L)
        runCurrent()

        saveRecordingSession()
        runCurrent()

        val savedShots = dao.getAllShotsOnce()
        assertEquals(2, savedShots.size)
        assertEquals(listOf("shot-125456", "shot-125457"), savedShots.map { it.id })
        assertTrue(savedShots[0].json.contains(""""id":"shot-125456""""))
        assertTrue(savedShots[1].json.contains(""""id":"shot-125457""""))
    }

    @Test
    fun afterSaveConfirmationStateReturnsToReady() = runTest(testDispatcher) {
        runCurrent()
        viewModel.onPrimaryAction()
        viewModel.onPrimaryAction()
        runCurrent()

        assertEquals(
            CaptureUiStateMapper.savedConfirmation(scaleConnectionLabel = "Scale: Connected"),
            viewModel.uiState.value
        )

        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(
            CaptureUiStateMapper.ready(scaleConnectionLabel = "Scale: Connected"),
            viewModel.uiState.value
        )
    }

    private fun TestScope.saveRecordingSession() {
        viewModel.onPrimaryAction()
        runCurrent()

        listOf(
            ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
            ScaleReading(timestampMillis = 1_000L, weightGrams = 1.0),
            ScaleReading(timestampMillis = 2_000L, weightGrams = 2.0),
            ScaleReading(timestampMillis = 3_000L, weightGrams = 3.5)
        ).forEach { reading ->
            scaleClient.emitReading(reading)
            runCurrent()
        }

        viewModel.onPrimaryAction()
        runCurrent()
    }
}

private class TestScaleClient : ScaleClient {
    private val connectionStates = MutableStateFlow<ScaleConnectionState>(
        ScaleConnectionState.Disconnected
    )
    override val connectionState: Flow<ScaleConnectionState> = connectionStates

    private val scaleReadings = MutableSharedFlow<ScaleReading>(extraBufferCapacity = 16)
    override val readings: Flow<ScaleReading> = scaleReadings

    override fun connect() {
        connectionStates.value = ScaleConnectionState.Connecting
        connectionStates.value = ScaleConnectionState.Connected
    }

    override fun disconnect() {
        connectionStates.value = ScaleConnectionState.Disconnected
    }

    fun emitConnectionState(connectionState: ScaleConnectionState) {
        connectionStates.value = connectionState
    }

    fun emitReading(reading: ScaleReading) {
        scaleReadings.tryEmit(reading)
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
