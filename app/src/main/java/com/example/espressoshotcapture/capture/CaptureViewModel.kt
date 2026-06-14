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
import com.example.espressoshotcapture.ble.BleScaleScanCandidate
import com.example.espressoshotcapture.capture.domain.CaptureTarget
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
    private val selectedDecentScaleCandidate: StateFlow<BleScaleScanCandidate?> =
        MutableStateFlow<BleScaleScanCandidate?>(null),
    private val createDecentScaleClient: (BleScaleScanCandidate) -> ScaleClient = { scaleClient },
    private val saveDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val savedConfirmationDelayMs: Long = 3_000L
) : ViewModel() {
    private var selectedScaleSource: CaptureScaleSource = CaptureScaleSource.FAKE
    private val _uiState = MutableStateFlow(
        CaptureUiStateMapper.initialDisconnectedReady(scaleModeLabel = scaleModeLabelForSource())
    )
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()
    private val _targetState = MutableStateFlow(MvpShotTarget.defaultState())
    val targetState: StateFlow<CaptureTargetState> = _targetState.asStateFlow()
    private var recordingReadingsJob: Job? = null
    private var fakeReadingEmissionJob: Job? = null
    private var recordingStartTimestampMs: Long? = null
    private var captureSessionStartedAtEpochMs: Long? = null
    private var firstScaleReadingTimestampMs: Long? = null
    private var lastCaptureSessionStartedAtEpochMs: Long? = null
    private var activeScaleClient: ScaleClient = scaleClient
    private var latestDecentScaleCandidate: BleScaleScanCandidate? = selectedDecentScaleCandidate.value
    private var activeScaleConnectionState: ScaleConnectionState = ScaleConnectionState.Disconnected
    private var realScaleHasReading: Boolean = false
    private var scaleConnectionJob: Job? = null
    private var realScaleReadinessJob: Job? = null
    private var activeCaptureTarget: CaptureTarget = requireNotNull(
        _targetState.value.toCaptureTargetOrNull()
    )
    private var shotCaptureEngine: ShotCaptureEngine = createArmedShotCaptureEngine(activeCaptureTarget)

    init {
        viewModelScope.launch {
            selectedDecentScaleCandidate.collect { candidate ->
                latestDecentScaleCandidate = candidate
                if (selectedScaleSource == CaptureScaleSource.DECENT && _uiState.value.status == CaptureStatus.READY) {
                    activateDecentScaleClient(candidate)
                } else {
                    updateCaptureSourceState()
                }
            }
        }
        selectFakeScaleSource()
    }

    fun onPrimaryAction() {
        when (_uiState.value.status) {
            CaptureStatus.READY -> startRecording()
            CaptureStatus.RECORDING -> stopAndSave()
            CaptureStatus.SAVED -> Unit
        }
    }

    fun selectFakeScaleSource() {
        if (_uiState.value.status != CaptureStatus.READY) return
        selectedScaleSource = CaptureScaleSource.FAKE
        realScaleHasReading = false
        activateScaleClient(scaleClient)
    }

    fun selectDecentScaleSource() {
        if (_uiState.value.status != CaptureStatus.READY) return
        selectedScaleSource = CaptureScaleSource.DECENT
        realScaleHasReading = false
        activateDecentScaleClient(latestDecentScaleCandidate)
    }

    fun updateTarget(doseGrams: Double, targetYieldGrams: Double) {
        _targetState.value = CaptureTargetState(
            doseGrams = doseGrams,
            targetYieldGrams = targetYieldGrams
        )
        updateReadyPrimaryActionEnabled()
    }

    fun updateDoseInput(input: String) {
        updateTarget(
            doseGrams = input.toDoubleOrNull() ?: Double.NaN,
            targetYieldGrams = _targetState.value.targetYieldGrams
        )
    }

    fun updateTargetYieldInput(input: String) {
        updateTarget(
            doseGrams = _targetState.value.doseGrams,
            targetYieldGrams = input.toDoubleOrNull() ?: Double.NaN
        )
    }

    private fun startRecording() {
        if (!canStartCapture()) {
            updateReadyPrimaryActionEnabled()
            return
        }
        val captureTarget = _targetState.value.toCaptureTargetOrNull() ?: return
        activeCaptureTarget = captureTarget
        recordingStartTimestampMs = null
        firstScaleReadingTimestampMs = null
        captureSessionStartedAtEpochMs = nextCaptureSessionStartedAtMs()
        shotCaptureEngine = createArmedShotCaptureEngine(activeCaptureTarget)
        _uiState.value = CaptureUiStateMapper.recording(
            scaleConnectionLabel = _uiState.value.scaleConnectionLabel,
            scaleModeLabel = scaleModeLabelForSource(),
            selectedScaleSource = selectedScaleSource,
            captureSourceStatusLabel = captureSourceStatusLabel(),
            captureSourceMessage = captureSourceMessage()
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
                scaleModeLabel = scaleModeLabelForSource(),
                selectedScaleSource = selectedScaleSource,
                captureSourceStatusLabel = captureSourceStatusLabel(),
                captureSourceMessage = captureSourceMessage()
            )
            delay(savedConfirmationDelayMs)
            _uiState.value = CaptureUiStateMapper.ready(
                scaleConnectionLabel = _uiState.value.scaleConnectionLabel,
                scaleModeLabel = scaleModeLabelForSource(),
                selectedScaleSource = selectedScaleSource,
                captureSourceStatusLabel = captureSourceStatusLabel(),
                captureSourceMessage = captureSourceMessage(),
                isPrimaryActionEnabled = canStartCapture()
            )
        }
    }

    private fun startRecordingReadingUpdates() {
        stopRecordingReadingUpdates()
        val captureScaleClient = activeScaleClient
        (captureScaleClient as? FakeScaleClient)?.resetReadings()

        recordingReadingsJob = viewModelScope.launch {
            captureScaleClient.readings.collect { reading ->
                if (_uiState.value.status == CaptureStatus.RECORDING) {
                    updateRecordingValues(reading)
                }
            }
        }

        val fakeScaleClient = captureScaleClient as? FakeScaleClient
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
        val captureElapsedMs = (sample.timestampMs - recordingStartMs).coerceAtLeast(0L)

        _uiState.value = _uiState.value.copy(
            currentWeightLabel = "Weight: ${sample.weightG.toOneDecimal()} g",
            progressLabel = activeCaptureTarget.progressLabel(sample.weightG),
            targetReachedLabel = activeCaptureTarget.targetReachedLabel(sample.weightG),
            captureElapsedLabel = "Capture elapsed: ${captureElapsedMs / 1_000L} s",
            averageFlowLabel = "Average flow: ${sample.averageFlowGPerS(captureElapsedMs).toOneDecimal()} g/s"
        )
    }

    private fun WeightSample.averageFlowGPerS(captureElapsedMs: Long): Double =
        if (captureElapsedMs <= 0L) {
            0.0
        } else {
            weightG / (captureElapsedMs / 1_000.0)
        }

    private fun Double.toOneDecimal(): String =
        ((this * 10.0).roundToInt() / 10.0).toString()

    private fun updateScaleConnectionLabel(scaleConnectionLabel: String) {
        _uiState.value = _uiState.value.copy(
            scaleConnectionLabel = scaleConnectionLabel,
            scaleModeLabel = scaleModeLabelForSource(),
            captureSourceStatusLabel = captureSourceStatusLabel(),
            captureSourceMessage = captureSourceMessage()
        )
        updateReadyPrimaryActionEnabled()
    }

    private fun updateReadyPrimaryActionEnabled() {
        if (_uiState.value.status == CaptureStatus.READY) {
            _uiState.value = _uiState.value.copy(
                isPrimaryActionEnabled = canStartCapture(),
                selectedScaleSource = selectedScaleSource,
                scaleModeLabel = scaleModeLabelForSource(),
                captureSourceStatusLabel = captureSourceStatusLabel(),
                captureSourceMessage = captureSourceMessage()
            )
        }
    }

    private fun activateDecentScaleClient(candidate: BleScaleScanCandidate?) {
        if (candidate == null) {
            scaleConnectionJob?.cancel()
            scaleConnectionJob = null
            realScaleReadinessJob?.cancel()
            realScaleReadinessJob = null
            activeScaleConnectionState = ScaleConnectionState.Disconnected
            activeScaleClient = scaleClient
            updateScaleConnectionLabel(ScaleConnectionState.Disconnected.toScaleConnectionLabel())
            return
        }

        activateScaleClient(createDecentScaleClient(candidate))
    }

    private fun activateScaleClient(client: ScaleClient) {
        activeScaleClient = client
        activeScaleConnectionState = ScaleConnectionState.Disconnected
        scaleConnectionJob?.cancel()
        realScaleReadinessJob?.cancel()
        realScaleReadinessJob = null

        scaleConnectionJob = viewModelScope.launch {
            client.connectionState.collect { connectionState ->
                if (activeScaleClient == client) {
                    activeScaleConnectionState = connectionState
                    updateScaleConnectionLabel(connectionState.toScaleConnectionLabel())
                }
            }
        }

        if (selectedScaleSource == CaptureScaleSource.DECENT) {
            realScaleReadinessJob = viewModelScope.launch {
                client.readings.collect {
                    if (activeScaleClient == client && selectedScaleSource == CaptureScaleSource.DECENT) {
                        realScaleHasReading = true
                        updateCaptureSourceState()
                    }
                }
            }
        }

        updateCaptureSourceState()
        client.connect()
    }

    private fun updateCaptureSourceState() {
        _uiState.value = _uiState.value.copy(
            selectedScaleSource = selectedScaleSource,
            scaleModeLabel = scaleModeLabelForSource(),
            captureSourceStatusLabel = captureSourceStatusLabel(),
            captureSourceMessage = captureSourceMessage()
        )
        updateReadyPrimaryActionEnabled()
    }

    private fun canStartCapture(): Boolean =
        _targetState.value.isValid && when (selectedScaleSource) {
            CaptureScaleSource.FAKE -> true
            CaptureScaleSource.DECENT ->
                latestDecentScaleCandidate != null &&
                    activeScaleConnectionState == ScaleConnectionState.Connected &&
                    realScaleHasReading
        }

    private fun captureSourceStatusLabel(): String =
        when (selectedScaleSource) {
            CaptureScaleSource.FAKE -> "Capture source: Fake scale/demo"
            CaptureScaleSource.DECENT -> when {
                latestDecentScaleCandidate == null -> "Capture source: Decent Scale/real unavailable"
                activeScaleConnectionState != ScaleConnectionState.Connected ->
                    "Capture source: Decent Scale/real not connected"
                !realScaleHasReading -> "Capture source: Decent Scale/real waiting for readings"
                else -> "Capture source: Decent Scale/real ready"
            }
        }

    private fun captureSourceMessage(): String? =
        when (selectedScaleSource) {
            CaptureScaleSource.FAKE -> null
            CaptureScaleSource.DECENT -> when {
                latestDecentScaleCandidate == null -> "Connect Decent Scale in BLE debug first."
                activeScaleConnectionState != ScaleConnectionState.Connected -> "Waiting for Decent Scale connection."
                !realScaleHasReading -> "Waiting for live scale readings."
                else -> null
            }
        }

    private fun scaleModeLabelForSource(): String? =
        if (selectedScaleSource == CaptureScaleSource.FAKE && scaleClient is FakeScaleClient) {
            "Fake scale simulation"
        } else {
            null
        }

    private fun ScaleConnectionState.toScaleConnectionLabel(): String =
        when (this) {
            ScaleConnectionState.Disconnected -> "Scale: Not connected"
            ScaleConnectionState.Connecting -> "Scale: Connecting"
            ScaleConnectionState.Connected -> "Scale: Connected"
            is ScaleConnectionState.Error -> "Scale: Error"
        }

    private fun createArmedShotCaptureEngine(target: CaptureTarget): ShotCaptureEngine =
        ShotCaptureEngine().also { engine ->
            engine.onScaleConnected()
            engine.onTareConfirmed()
            engine.arm(target)
        }

    private fun CaptureTarget.progressLabel(currentWeightG: Double): String =
        "Progress: ${currentWeightG.toOneDecimal()} / ${targetYieldG.toOneDecimal()} g"

    private fun CaptureTarget.targetReachedLabel(currentWeightG: Double): String =
        if (currentWeightG >= targetYieldG) {
            MvpShotTarget.TARGET_REACHED_LABEL
        } else {
            MvpShotTarget.TARGET_NOT_REACHED_LABEL
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
        scaleConnectionJob?.cancel()
        realScaleReadinessJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(
            shotRepository: ShotRepository,
            scaleClient: ScaleClient,
            selectedDecentScaleCandidate: StateFlow<BleScaleScanCandidate?> =
                MutableStateFlow<BleScaleScanCandidate?>(null),
            createDecentScaleClient: (BleScaleScanCandidate) -> ScaleClient = { scaleClient }
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CaptureViewModel::class.java)) {
                        return CaptureViewModel(
                            shotRepository = shotRepository,
                            scaleClient = scaleClient,
                            selectedDecentScaleCandidate = selectedDecentScaleCandidate,
                            createDecentScaleClient = createDecentScaleClient
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
            scaleClient = application.appContainer.scaleClient,
            selectedDecentScaleCandidate = application.appContainer.selectedDecentScaleCandidate,
            createDecentScaleClient = application.appContainer::createDecentScaleClient
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
    val targetState by viewModel.targetState.collectAsState()

    CaptureScreen(
        uiState = uiState,
        targetState = targetState,
        onDoseChanged = viewModel::updateDoseInput,
        onTargetYieldChanged = viewModel::updateTargetYieldInput,
        onFakeScaleSelected = viewModel::selectFakeScaleSource,
        onDecentScaleSelected = viewModel::selectDecentScaleSource,
        onPrimaryAction = viewModel::onPrimaryAction,
        modifier = modifier
    )
}
