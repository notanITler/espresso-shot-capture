package com.example.espressoshotcapture.repository

import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.capture.domain.ShotUserMetadata
import com.example.espressoshotcapture.capture.domain.TasteDirection
import com.example.espressoshotcapture.export.ShotDraftJsonExporter
import com.example.espressoshotcapture.persistence.ShotEntity

object ShotEntityMapper {
    fun fromShotDraft(
        shotDraft: ShotDraft,
        userMetadata: ShotUserMetadata = ShotUserMetadata()
    ): ShotEntity =
        ShotEntity(
            id = shotDraft.id,
            json = ShotDraftJsonExporter.export(shotDraft),
            createdAtEpochMillis = shotDraft.createdAtEpochMs,
            rating = userMetadata.rating,
            tasteDirection = userMetadata.tasteDirection?.name,
            grindSetting = userMetadata.grindSetting,
            beanName = userMetadata.beanName,
            notes = userMetadata.notes
        )

    fun toUserMetadata(entity: ShotEntity): ShotUserMetadata =
        ShotUserMetadata(
            rating = entity.rating,
            tasteDirection = entity.tasteDirection?.let(::tasteDirectionOrNull),
            grindSetting = entity.grindSetting,
            beanName = entity.beanName,
            notes = entity.notes
        )

    fun hasValidUserMetadata(entity: ShotEntity): Boolean =
        (entity.tasteDirection == null || tasteDirectionOrNull(entity.tasteDirection) != null) &&
            toUserMetadata(entity).isValid()

    private fun tasteDirectionOrNull(value: String): TasteDirection? =
        TasteDirection.entries.firstOrNull { direction -> direction.name == value }
}
