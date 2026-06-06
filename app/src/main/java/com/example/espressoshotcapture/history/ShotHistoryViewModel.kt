package com.example.espressoshotcapture.history

import androidx.lifecycle.ViewModel
import com.example.espressoshotcapture.persistence.ShotEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShotHistoryViewModel(
    initialShots: List<ShotEntity> = emptyList()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ShotHistoryStateMapper.fromEntities(initialShots)
    )

    val uiState: StateFlow<ShotHistoryUiState> = _uiState.asStateFlow()
}
