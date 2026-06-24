package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity
import com.example.espressoshotcapture.repository.ShotEntityMapper

object ShotHistoryStateMapper {
    fun fromEntities(
        entities: List<ShotEntity>,
        selectedShotId: String? = null
    ): ShotHistoryUiState {
        val selectedEntity = entities.firstOrNull { entity -> entity.id == selectedShotId }
        return ShotHistoryUiState(
            items = ShotHistoryMapper.fromEntities(entities),
            selectedShotDetail = selectedEntity?.toDetail(),
            metadataEditor = selectedEntity?.toMetadataEditor()
        )
    }

    private fun ShotEntity.toMetadataEditor(): ShotUserMetadataEditorState {
        val metadata = ShotEntityMapper.toUserMetadata(this)
        return ShotUserMetadataEditorState(
            shotId = id,
            ratingText = metadata.rating?.toString().orEmpty(),
            tasteDirection = metadata.tasteDirection,
            grindSetting = metadata.grindSetting.orEmpty(),
            beanName = metadata.beanName.orEmpty(),
            notes = metadata.notes.orEmpty()
        )
    }

    private fun ShotEntity.toDetail(): ShotHistoryDetail =
        ShotHistoryMapper.summaryFromJson(json).let { summary ->
            ShotHistoryDetail(
                id = id,
                createdAtEpochMillis = createdAtEpochMillis,
                json = json,
                createdLabel = ShotHistoryMapper.createdLabel(createdAtEpochMillis),
                sourceLabel = summary.sourceLabel,
                qualityLabel = summary.qualityLabel,
                finalYieldLabel = summary.finalYieldLabel,
                flowTimeLabel = summary.flowTimeLabel,
                sampleCountLabel = summary.sampleCountLabel,
                doseLabel = summary.doseLabel,
                targetYieldLabel = summary.targetYieldLabel,
                ratioLabel = summary.ratioLabel,
                averageFlowLabel = summary.averageFlowLabel,
                targetReachedLabel = summary.targetReachedLabel
            )
        }
}
