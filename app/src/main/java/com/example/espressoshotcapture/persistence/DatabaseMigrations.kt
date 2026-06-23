package com.example.espressoshotcapture.persistence

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE shots ADD COLUMN rating INTEGER")
        database.execSQL("ALTER TABLE shots ADD COLUMN tasteDirection TEXT")
        database.execSQL("ALTER TABLE shots ADD COLUMN grindSetting TEXT")
        database.execSQL("ALTER TABLE shots ADD COLUMN beanName TEXT")
        database.execSQL("ALTER TABLE shots ADD COLUMN notes TEXT")
    }
}
