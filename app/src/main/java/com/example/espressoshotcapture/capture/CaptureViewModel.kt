package com.example.espressoshotcapture.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaptureViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CaptureUiStateMapper.initialDisconnectedReady())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()
}

@Composable
fun CaptureRoute(
    modifier: Modifier = Modifier
) {
    val viewModel: CaptureViewModel = viewModel()

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
        modifier = modifier
    )
}
