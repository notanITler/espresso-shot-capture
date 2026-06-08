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
        ShotHistoryMapper.fromEntity(this).let { item ->
            ShotHistoryDetail(
                id = id,
                createdAtEpochMillis = createdAtEpochMillis,
                json = json,
                finalYieldLabel = item.finalYieldLabel,
                flowTimeLabel = item.flowTimeLabel,
                targetYieldLabel = item.targetYieldLabel
            )
        }
}
