package com.example.espressoshotcapture.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shots")
data class ShotEntity(
    @PrimaryKey val id: String,
    val json: String,
    val createdAtEpochMillis: Long
)
