package com.example.espressoshotcapture.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.espressoshotcapture.capture.domain.ShotUserMetadata
import com.example.espressoshotcapture.capture.domain.ShotUserMetadataValidator
import com.example.espressoshotcapture.capture.domain.TasteDirection
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShotHistoryViewModel(
    private val shotRepository: ShotRepository,
    private val metadataWriteDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val selectedShotId = MutableStateFlow<String?>(null)
    private val metadataEditorOverride = MutableStateFlow<ShotUserMetadataEditorState?>(null)

    val uiState: StateFlow<ShotHistoryUiState> =
        combine(
            shotRepository.observeShots(),
            selectedShotId,
            metadataEditorOverride
        ) { entities, selectedId, editorOverride ->
            val mappedState = ShotHistoryStateMapper.fromEntities(
                entities = entities,
                selectedShotId = selectedId
            )
            mappedState.copy(
                metadataEditor = if (editorOverride?.shotId == selectedId) {
                    editorOverride
                } else {
                    mappedState.metadataEditor
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = ShotHistoryUiState(items = emptyList())
        )

    fun selectShot(id: String) {
        selectedShotId.value = id
        metadataEditorOverride.value = null
    }

    fun updateMetadataRating(value: String) {
        updateMetadataEditor { editor ->
            editor.copy(ratingText = value, validationMessage = null)
        }
    }

    fun updateMetadataTasteDirection(value: TasteDirection?) {
        updateMetadataEditor { editor ->
            editor.copy(tasteDirection = value, validationMessage = null)
        }
    }

    fun updateMetadataGrindSetting(value: String) {
        updateMetadataEditor { editor ->
            editor.copy(grindSetting = value, validationMessage = null)
        }
    }

    fun updateMetadataBeanName(value: String) {
        updateMetadataEditor { editor ->
            editor.copy(beanName = value, validationMessage = null)
        }
    }

    fun updateMetadataNotes(value: String) {
        updateMetadataEditor { editor ->
            editor.copy(notes = value, validationMessage = null)
        }
    }

    fun saveShotUserMetadata() {
        val editor = currentMetadataEditor() ?: return
        val metadata = editor.toUserMetadataOrNull()
        if (metadata == null) {
            metadataEditorOverride.value = editor.copy(
                validationMessage = editor.validationMessageForInvalidInput()
            )
            return
        }

        viewModelScope.launch {
            val result = runCatching {
                withContext(metadataWriteDispatcher) {
                    shotRepository.updateShotUserMetadata(editor.shotId, metadata)
                }
            }
            metadataEditorOverride.value = editor.copy(
                validationMessage = result.fold(
                    onSuccess = { wasUpdated ->
                        if (wasUpdated) {
                            "Shot feedback saved"
                        } else {
                            "Shot no longer exists"
                        }
                    },
                    onFailure = { "Could not save shot feedback" }
                )
            )
        }
    }

    fun clearShotUserMetadata() {
        val editor = currentMetadataEditor() ?: return
        val clearedEditor = ShotUserMetadataEditorState(shotId = editor.shotId)
        viewModelScope.launch {
            val result = runCatching {
                withContext(metadataWriteDispatcher) {
                    shotRepository.updateShotUserMetadata(
                        shotId = editor.shotId,
                        metadata = ShotUserMetadata()
                    )
                }
            }
            metadataEditorOverride.value = clearedEditor.copy(
                validationMessage = result.fold(
                    onSuccess = { wasUpdated ->
                        if (wasUpdated) {
                            "Shot feedback cleared"
                        } else {
                            "Shot no longer exists"
                        }
                    },
                    onFailure = { "Could not clear shot feedback" }
                )
            )
        }
    }

    private fun updateMetadataEditor(
        transform: (ShotUserMetadataEditorState) -> ShotUserMetadataEditorState
    ) {
        val editor = currentMetadataEditor() ?: return
        metadataEditorOverride.value = transform(editor)
    }

    private fun currentMetadataEditor(): ShotUserMetadataEditorState? {
        val selectedId = selectedShotId.value
        return metadataEditorOverride.value?.takeIf { editor -> editor.shotId == selectedId }
            ?: uiState.value.metadataEditor
    }

    private fun ShotUserMetadataEditorState.toUserMetadataOrNull(): ShotUserMetadata? {
        val trimmedRating = ratingText.trim()
        val rating = when {
            trimmedRating.isBlank() -> null
            else -> trimmedRating
                .toIntOrNull()
                ?.takeIf(ShotUserMetadataValidator::isValidRating)
                ?: return null
        }
        val metadata = ShotUserMetadata(
            rating = rating,
            tasteDirection = tasteDirection,
            grindSetting = grindSetting.trim().ifBlank { null },
            beanName = beanName.trim().ifBlank { null },
            notes = notes.trim().ifBlank { null }
        )
        return metadata.takeIf { it.isValid() }
    }

    private fun ShotUserMetadataEditorState.validationMessageForInvalidInput(): String =
        when {
            ratingText.isNotBlank() &&
                ratingText.trim().toIntOrNull()?.let(ShotUserMetadataValidator::isValidRating) != true ->
                "Rating must be 1-5"
            !ShotUserMetadataValidator.isValidGrindSetting(grindSetting) ->
                "Grind setting must be a decimal value"
            else -> "Shot feedback is invalid"
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
