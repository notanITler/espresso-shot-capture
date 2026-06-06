package com.example.espressoshotcapture.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShot(shot: ShotEntity)

    @Query("SELECT * FROM shots WHERE id = :id LIMIT 1")
    fun getShotById(id: String): ShotEntity?

    @Query("SELECT * FROM shots ORDER BY createdAtEpochMillis ASC")
    fun getAllShots(): List<ShotEntity>

    @Query("DELETE FROM shots WHERE id = :id")
    fun deleteShotById(id: String)
}
