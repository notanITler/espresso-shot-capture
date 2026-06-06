package com.example.espressoshotcapture.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.espressoshotcapture.persistence.EspressoShotDatabase
import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShotRepositoryTest {
    private lateinit var database: EspressoShotDatabase
    private lateinit var dao: ShotDao
    private lateinit var repository: ShotRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EspressoShotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.shotDao()
        repository = ShotRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observingShotsEmitsInsertedShots() = runTest {
        val shot = shotEntity(id = "shot-1")

        repository.saveShot(shot)

        assertEquals(listOf(shot), repository.observeShots().first())
    }

    @Test
    fun insertedEntityDataIsPreserved() = runTest {
        val shot = shotEntity(
            id = "shot-1",
            json = """{"schemaVersion":1,"shot":{"id":"shot-1"}}""",
            createdAtEpochMillis = 1_234L
        )

        repository.saveShot(shot)

        assertEquals(shot, dao.getShotById("shot-1"))
    }

    private fun shotEntity(
        id: String,
        json: String = """{"schemaVersion":1,"shot":{"id":"$id"}}""",
        createdAtEpochMillis: Long = 1_000L
    ): ShotEntity =
        ShotEntity(
            id = id,
            json = json,
            createdAtEpochMillis = createdAtEpochMillis
        )
}
