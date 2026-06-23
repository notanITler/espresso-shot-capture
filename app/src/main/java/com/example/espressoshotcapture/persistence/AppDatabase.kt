package com.example.espressoshotcapture.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ShotEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shotDao(): ShotDao
}
