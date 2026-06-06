package com.example.espressoshotcapture.repository

import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.export.ShotDraftJsonExporter
import com.example.espressoshotcapture.persistence.ShotEntity

object ShotEntityMapper {
    fun fromShotDraft(shotDraft: ShotDraft): ShotEntity =
        ShotEntity(
            id = shotDraft.id,
            json = ShotDraftJsonExporter.export(shotDraft),
            createdAtEpochMillis = shotDraft.createdAtEpochMs
        )
}
