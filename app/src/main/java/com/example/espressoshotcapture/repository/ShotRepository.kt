package com.example.espressoshotcapture.repository

import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import kotlinx.coroutines.flow.Flow

class ShotRepository(
    private val shotDao: ShotDao
) {
    fun observeShots(): Flow<List<ShotEntity>> =
        shotDao.observeShots()

    fun saveShot(entity: ShotEntity) {
        shotDao.insertShot(entity)
    }

    fun saveShotDraft(shotDraft: ShotDraft) {
        saveShot(ShotEntityMapper.fromShotDraft(shotDraft))
    }
}
