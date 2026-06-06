package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity

object ShotHistoryStateMapper {
    fun fromEntities(entities: List<ShotEntity>): ShotHistoryUiState =
        ShotHistoryUiState(
            items = ShotHistoryMapper.fromEntities(entities)
        )
}
