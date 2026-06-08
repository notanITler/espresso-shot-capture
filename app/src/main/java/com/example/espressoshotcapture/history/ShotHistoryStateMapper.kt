package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity

object ShotHistoryStateMapper {
    fun fromEntities(
        entities: List<ShotEntity>,
        selectedShotId: String? = null
    ): ShotHistoryUiState =
        ShotHistoryUiState(
            items = ShotHistoryMapper.fromEntities(entities),
            selectedShotDetail = entities
                .firstOrNull { entity -> entity.id == selectedShotId }
                ?.toDetail()
        )

    private fun ShotEntity.toDetail(): ShotHistoryDetail =
        ShotHistoryMapper.summaryFromJson(json).let { summary ->
            ShotHistoryDetail(
                id = id,
                createdAtEpochMillis = createdAtEpochMillis,
                json = json,
                finalYieldLabel = summary.finalYieldLabel,
                flowTimeLabel = summary.flowTimeLabel,
                targetYieldLabel = summary.targetYieldLabel,
                averageFlowLabel = summary.averageFlowLabel,
                targetReachedLabel = summary.targetReachedLabel
            )
        }
}
