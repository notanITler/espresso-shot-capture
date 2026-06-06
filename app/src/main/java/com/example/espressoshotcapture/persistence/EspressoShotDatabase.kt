package com.example.espressoshotcapture.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ShotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EspressoShotDatabase : RoomDatabase() {
    abstract fun shotDao(): ShotDao
}
