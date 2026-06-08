package com.example.espressoshotcapture.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ShotHistoryViewModel(
    shotRepository: ShotRepository
) : ViewModel() {
    private val selectedShotId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ShotHistoryUiState> =
        shotRepository.observeShots()
            .combine(selectedShotId) { entities, selectedId ->
                ShotHistoryStateMapper.fromEntities(
                    entities = entities,
                    selectedShotId = selectedId
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
                initialValue = ShotHistoryUiState(items = emptyList())
            )

    fun selectShot(id: String) {
        selectedShotId.value = id
    }

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
