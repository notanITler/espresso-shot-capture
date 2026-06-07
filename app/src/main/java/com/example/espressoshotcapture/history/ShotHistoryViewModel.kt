package com.example.espressoshotcapture.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ShotHistoryViewModel(
    shotRepository: ShotRepository
) : ViewModel() {
    val uiState: StateFlow<ShotHistoryUiState> =
        shotRepository.observeShots()
            .map { entities -> ShotHistoryStateMapper.fromEntities(entities) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
                initialValue = ShotHistoryUiState(items = emptyList())
            )

    companion object {
        fun factory(shotRepository: ShotRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ShotHistoryViewModel::class.java)) {
                        return ShotHistoryViewModel(shotRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
