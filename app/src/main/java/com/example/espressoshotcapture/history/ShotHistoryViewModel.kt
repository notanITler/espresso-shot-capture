package com.example.espressoshotcapture.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.capture.domain.ShotResult
import com.example.espressoshotcapture.capture.domain.ShotSource
import com.example.espressoshotcapture.capture.domain.ShotStatus
import com.example.espressoshotcapture.capture.domain.ShotTiming
import com.example.espressoshotcapture.capture.domain.StartMode
import com.example.espressoshotcapture.capture.domain.StopMode
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShotHistoryViewModel(
    private val shotRepository: ShotRepository,
    private val saveDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    val uiState: StateFlow<ShotHistoryUiState> =
        shotRepository.observeShots()
            .map { entities -> ShotHistoryStateMapper.fromEntities(entities) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
                initialValue = ShotHistoryUiState(items = emptyList())
            )

    fun addTestShot() {
        viewModelScope.launch(saveDispatcher) {
            shotRepository.saveShotDraft(createTestShotDraft())
        }
    }

    private fun createTestShotDraft(): ShotDraft {
        val createdAtEpochMs = System.currentTimeMillis()
        val id = "test-shot-$createdAtEpochMs"

        return ShotDraft(
            id = id,
            createdAtEpochMs = createdAtEpochMs,
            target = CaptureTarget(
                source = ShotSource.QUICK_SHOT,
                recipeId = null,
                beanId = null,
                doseG = 18.0,
                targetRatio = 2.0,
                targetYieldG = 36.0,
                targetTimeS = null
            ),
            timing = ShotTiming(
                startMode = StartMode.AUTO_WEIGHT,
                stopMode = StopMode.TARGET_YIELD,
                brewTimeMs = null,
                flowTimeMs = 0L,
                targetReachedAtMs = null,
                firstWeightDelayMs = null,
                postTargetRecordingMs = 1_500L
            ),
            result = ShotResult(
                actualYieldG = null,
                postTargetDriftG = null,
                averageFlowGPerS = null,
                maxFlowGPerS = null,
                sampleCount = 0
            ),
            samples = emptyList(),
            status = ShotStatus.COMPLETED,
            notes = "Debug test shot"
        )
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
