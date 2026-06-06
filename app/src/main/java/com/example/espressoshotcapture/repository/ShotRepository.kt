package com.example.espressoshotcapture.repository

import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.export.ShotDraftJsonExporter
import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity

class ShotRepository(
    private val shotDao: ShotDao
) {
    suspend fun saveShot(shotDraft: ShotDraft, createdAtEpochMillis: Long) {
        val exportedJson = ShotDraftJsonExporter.export(shotDraft)
        shotDao.insertShot(
            ShotEntity(
                id = shotDraft.id,
                json = exportedJson,
                createdAtEpochMillis = createdAtEpochMillis
            )
        )
    }

    suspend fun getShotById(id: String): ShotEntity? =
        shotDao.getShotById(id)

    suspend fun getAllShots(): List<ShotEntity> =
        shotDao.getAllShots()

    suspend fun deleteShotById(id: String) {
        shotDao.deleteShotById(id)
    }
}
