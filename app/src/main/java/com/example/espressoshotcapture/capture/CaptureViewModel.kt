package com.example.espressoshotcapture.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication
import com.example.espressoshotcapture.capture.domain.FakeScaleClient
import com.example.espressoshotcapture.capture.domain.ScaleClient
import com.example.espressoshotcapture.capture.domain.ScaleConnectionState
import com.example.espressoshotcapture.capture.domain.ScaleReading
import com.example.espressoshotcapture.capture.domain.ScaleReadingMapper
import com.example.espressoshotcapture.capture.domain.WeightSample
import com.example.espressoshotcapture.capture.engine.ShotCaptureEngine
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class CaptureViewModel(
    private val shotRepository: ShotRepository,
    private val scaleClient: ScaleClient,
    private val saveDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val savedConfirmationDelayMs: Long = 3_000L
) : ViewModel() {
    private val scaleModeLabel: String? = if (scaleClient is FakeScaleClient) {
        "Fake scale simulation"
    } else {
        null
    }
    private val _uiState = MutableStateFlow(
        CaptureUiStateMapper.initialDisconnectedReady(scaleModeLabel = scaleModeLabel)
    )
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()
    private var recordingReadingsJob: Job? = null
    private var fakeReadingEmissionJob: Job? = null
    private var recordingStartTimestampMs: Long? = null
    private var captureSessionStartedAtEpochMs: Long? = null
    private var firstScaleReadingTimestampMs: Long? = null
    private var lastCaptureSessionStartedAtEpochMs: Long? = null
    private var shotCaptureEngine: ShotCaptureEngine = createArmedShotCaptureEngine()

    init {
        viewModelScope.launch {
            scaleClient.connectionState.collect { connectionState ->
                updateScaleConnectionLabel(connectionState.toScaleConnectionLabel())
            }
        }
        scaleClient.connect()
    }

    fun onPrimaryAction() {
        when (_uiState.value.status) {
            CaptureStatus.READY -> startRecording()
            CaptureStatus.RECORDING -> stopAndSave()
            CaptureStatus.SAVED -> Unit
        }
    }

    private fun startRecording() {
        recordingStartTimestampMs = null
        firstScaleReadingTimestampMs = null
        captureSessionStartedAtEpochMs = nextCaptureSessionStartedAtMs()
        shotCaptureEngine = createArmedShotCaptureEngine()
        _uiState.value = CaptureUiStateMapper.recording(
            scaleConnectionLabel = _uiState.value.scaleConnectionLabel,
            scaleModeLabel = _uiState.value.scaleModeLabel
        )
        startRecordingReadingUpdates()
    }

    private fun stopAndSave() {
        stopRecordingReadingUpdates()
        viewModelScope.launch(saveDispatcher) {
            val fallbackCreatedAtMs = captureSessionStartedAtEpochMs ?: nextCaptureSessionStartedAtMs()
            val shotDraft = shotCaptureEngine.completedShotDraft
                ?: shotCaptureEngine.stopManually(fallbackCreatedAtEpochMs = fallbackCreatedAtMs)
                ?: error("Unable to create shot draft from capture engine")
            shotRepository.saveShotDraft(shotDraft)
            _uiState.value = CaptureUiStateMapper.savedConfirmation(
                scaleConnectionLabel = _uiState.value.scaleConnectionLabel,
                scaleModeLabel = _uiState.value.scaleModeLabel
            )
            delay(savedConfirmationDelayMs)
            _uiState.value = CaptureUiStateMapper.ready(
                scaleConnectionLabel = _uiState.value.scaleConnectionLabel,
                scaleModeLabel = _uiState.value.scaleModeLabel
            )
        }
    }

    private fun startRecordingReadingUpdates() {
        stopRecordingReadingUpdates()
        (scaleClient as? FakeScaleClient)?.resetReadings()

        recordingReadingsJob = viewModelScope.launch {
            scaleClient.readings.collect { reading ->
                if (_uiState.value.status == CaptureStatus.RECORDING) {
                    updateRecordingValues(reading)
                }
            }
        }

        val fakeScaleClient = scaleClient as? FakeScaleClient
        if (fakeScaleClient != null) {
            fakeReadingEmissionJob = viewModelScope.launch {
                while (isActive && _uiState.value.status == CaptureStatus.RECORDING) {
                    fakeScaleClient.emitNextReading()
                    delay(500L)
                }
            }
        }
    }

    private fun stopRecordingReadingUpdates() {
        recordingReadingsJob?.cancel()
        recordingReadingsJob = null
        fakeReadingEmissionJob?.cancel()
        fakeReadingEmissionJob = null
    }

    private fun updateRecordingValues(reading: ScaleReading) {
        val sample = ScaleReadingMapper.toWeightSample(reading)
        shotCaptureEngine.onWeightSample(sample.toCaptureSessionSample())
        val recordingStartMs = recordingStartTimestampMs ?: sample.timestampMs.also {
            recordingStartTimestampMs = it
        }
        val flowTimeMs = (sample.timestampMs - recordingStartMs).coerceAtLeast(0L)

        _uiState.value = _uiState.value.copy(
            currentWeightLabel = "Weight: ${sample.weightG.toOneDecimal()} g",
            progressLabel = MvpShotTarget.progressLabel(sample.weightG),
            targetReachedLabel = MvpShotTarget.targetReachedLabel(sample.weightG),
            flowTimeLabel = "Flow time: ${flowTimeMs / 1_000L} s",
            averageFlowLabel = "Average flow: ${sample.averageFlowGPerS(flowTimeMs).toOneDecimal()} g/s"
        )
    }

    private fun WeightSample.averageFlowGPerS(flowTimeMs: Long): Double =
        if (flowTimeMs <= 0L) {
            0.0
        } else {
            weightG / (flowTimeMs / 1_000.0)
        }

    private fun Double.toOneDecimal(): String =
        ((this * 10.0).roundToInt() / 10.0).toString()

    private fun updateScaleConnectionLabel(scaleConnectionLabel: String) {
        _uiState.value = _uiState.value.copy(
            scaleConnectionLabel = scaleConnectionLabel
        )
    }

    private fun ScaleConnectionState.toScaleConnectionLabel(): String =
        when (this) {
            ScaleConnectionState.Disconnected -> "Scale: Not connected"
            ScaleConnectionState.Connecting -> "Scale: Connecting"
            ScaleConnectionState.Connected -> "Scale: Connected"
            is ScaleConnectionState.Error -> "Scale: Error"
        }

    private fun createArmedShotCaptureEngine(): ShotCaptureEngine =
        ShotCaptureEngine().also { engine ->
            engine.onScaleConnected()
            engine.onTareConfirmed()
            engine.arm(MvpShotTarget.toCaptureTarget())
        }

    private fun nextCaptureSessionStartedAtMs(): Long {
        val nowMs = currentTimeMillis()
        val previousSessionStartedAtMs = lastCaptureSessionStartedAtEpochMs
        val sessionStartedAtMs = if (
            previousSessionStartedAtMs != null &&
            nowMs <= previousSessionStartedAtMs
        ) {
            previousSessionStartedAtMs + 1L
        } else {
            nowMs
        }
        lastCaptureSessionStartedAtEpochMs = sessionStartedAtMs
        return sessionStartedAtMs
    }

    private fun WeightSample.toCaptureSessionSample(): WeightSample {
        val sessionStartedAtMs = captureSessionStartedAtEpochMs ?: nextCaptureSessionStartedAtMs()
        val firstReadingTimestampMs = firstScaleReadingTimestampMs ?: timestampMs.also {
            firstScaleReadingTimestampMs = it
        }
        val elapsedMs = (timestampMs - firstReadingTimestampMs).coerceAtLeast(0L)
        return copy(timestampMs = sessionStartedAtMs + elapsedMs)
    }

    override fun onCleared() {
        stopRecordingReadingUpdates()
        super.onCleared()
    }

    companion object {
        fun factory(
            shotRepository: ShotRepository,
            scaleClient: ScaleClient
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CaptureViewModel::class.java)) {
                        return CaptureViewModel(
                            shotRepository = shotRepository,
                            scaleClient = scaleClient
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

@Composable
fun CaptureRoute(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as EspressoShotCaptureApplication
    val viewModel: CaptureViewModel = viewModel(
        factory = CaptureViewModel.factory(
            shotRepository = application.appContainer.shotRepository,
            scaleClient = application.appContainer.scaleClient
        )
    )

    CaptureRoute(
        viewModel = viewModel,
        modifier = modifier
    )
}

@Composable
fun CaptureRoute(
    viewModel: CaptureViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    CaptureScreen(
        uiState = uiState,
        onPrimaryAction = viewModel::onPrimaryAction,
        modifier = modifier
    )
}
