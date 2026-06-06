package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity

object ShotHistoryMapper {
    fun fromEntity(entity: ShotEntity): ShotHistoryItem =
        ShotHistoryItem(
            id = entity.id,
            createdAtEpochMillis = entity.createdAtEpochMillis
        )

    fun fromEntities(entities: List<ShotEntity>): List<ShotHistoryItem> =
        entities.map(::fromEntity)
}
