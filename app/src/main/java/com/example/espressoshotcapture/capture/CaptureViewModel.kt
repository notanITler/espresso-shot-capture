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
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CaptureViewModel(
    private val shotRepository: ShotRepository,
    private val saveDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val savedConfirmationDelayMs: Long = 3_000L
) : ViewModel() {
    private val _uiState = MutableStateFlow(CaptureUiStateMapper.initialDisconnectedReady())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun onPrimaryAction() {
        when (_uiState.value.status) {
            CaptureStatus.READY -> startRecording()
            CaptureStatus.RECORDING -> stopAndSave()
            CaptureStatus.SAVED -> Unit
        }
    }

    private fun startRecording() {
        _uiState.value = CaptureUiStateMapper.recording()
    }

    private fun stopAndSave() {
        viewModelScope.launch(saveDispatcher) {
            val shotDraft = FakeCaptureShotDraftFactory.create(currentTimeMillis())
            shotRepository.saveShotDraft(shotDraft)
            _uiState.value = CaptureUiStateMapper.savedConfirmation()
            delay(savedConfirmationDelayMs)
            _uiState.value = CaptureUiStateMapper.initialDisconnectedReady()
        }
    }

    companion object {
        fun factory(shotRepository: ShotRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CaptureViewModel::class.java)) {
                        return CaptureViewModel(shotRepository) as T
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
        factory = CaptureViewModel.factory(application.appContainer.shotRepository)
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
