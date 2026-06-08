package com.example.espressoshotcapture.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShot(entity: ShotEntity)

    @Query("SELECT * FROM shots ORDER BY createdAtEpochMillis DESC")
    fun observeShots(): Flow<List<ShotEntity>>

    @Query("SELECT * FROM shots WHERE id = :id LIMIT 1")
    fun getShotById(id: String): ShotEntity?

    @Query("SELECT * FROM shots ORDER BY createdAtEpochMillis DESC")
    fun getAllShotsOnce(): List<ShotEntity>

    fun getAllShots(): List<ShotEntity> = getAllShotsOnce()

    @Query("DELETE FROM shots WHERE id = :id")
    fun deleteShotById(id: String)
}
