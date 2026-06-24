package com.example.espressoshotcapture.repository

import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.capture.domain.ShotUserMetadata
import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import kotlinx.coroutines.flow.Flow

class ShotRepository(
    private val shotDao: ShotDao
) {
    fun observeShots(): Flow<List<ShotEntity>> =
        shotDao.observeShots()

    fun saveShot(entity: ShotEntity) {
        require(ShotEntityMapper.hasValidUserMetadata(entity)) {
            "Shot metadata is invalid"
        }
        shotDao.insertShot(entity)
    }

    fun saveShotDraft(
        shotDraft: ShotDraft,
        userMetadata: ShotUserMetadata = ShotUserMetadata()
    ) {
        require(userMetadata.isValid()) {
            "Shot metadata is invalid"
        }
        saveShot(ShotEntityMapper.fromShotDraft(shotDraft, userMetadata))
    }

    fun updateShotUserMetadata(shotId: String, metadata: ShotUserMetadata): Boolean {
        require(metadata.isValid()) {
            "Shot metadata is invalid"
        }

        val updatedRows = shotDao.updateShotUserMetadata(
            id = shotId,
            rating = metadata.rating,
            tasteDirection = metadata.tasteDirection?.name,
            grindSetting = metadata.grindSetting,
            beanName = metadata.beanName,
            notes = metadata.notes
        )
        return updatedRows > 0
    }
}
