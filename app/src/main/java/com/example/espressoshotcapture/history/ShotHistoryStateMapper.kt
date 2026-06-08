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
        ShotHistoryDetail(
            id = id,
            createdAtEpochMillis = createdAtEpochMillis,
            json = json
        )
}
