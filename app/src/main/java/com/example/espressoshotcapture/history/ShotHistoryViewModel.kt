package com.example.espressoshotcapture.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.espressoshotcapture.persistence.ShotEntity
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
            val createdAtEpochMillis = System.currentTimeMillis()
            val id = "test-shot-$createdAtEpochMillis"

            shotRepository.saveShot(
                ShotEntity(
                    id = id,
                    json = testShotJson(
                        id = id,
                        createdAtEpochMillis = createdAtEpochMillis
                    ),
                    createdAtEpochMillis = createdAtEpochMillis
                )
            )
        }
    }

    private fun testShotJson(
        id: String,
        createdAtEpochMillis: Long
    ): String =
        """{"schemaVersion":1,"shot":{"id":"$id","createdAtEpochMs":$createdAtEpochMillis,"samples":[],"status":"COMPLETED","notes":null}}"""

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
