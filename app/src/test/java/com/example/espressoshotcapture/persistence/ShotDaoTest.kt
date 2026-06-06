package com.example.espressoshotcapture.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.CapturedSample
import com.example.espressoshotcapture.capture.domain.SampleSource
import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.capture.domain.ShotResult
import com.example.espressoshotcapture.capture.domain.ShotSource
import com.example.espressoshotcapture.capture.domain.ShotStatus
import com.example.espressoshotcapture.capture.domain.ShotTiming
import com.example.espressoshotcapture.capture.domain.StartMode
import com.example.espressoshotcapture.capture.domain.StopMode
import com.example.espressoshotcapture.export.ShotDraftJsonExporter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShotDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ShotDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.shotDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndReadShotById() {
        val shot = shotEntity(id = "shot-1")

        dao.insertShot(shot)

        assertEquals(shot, dao.getShotById("shot-1"))
    }

    @Test
    fun getAllShotsReturnsInsertedShots() {
        val first = shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L)
        val second = shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L)

        dao.insertShot(second)
        dao.insertShot(first)

        assertEquals(listOf(first, second), dao.getAllShots())
    }

    @Test
    fun deleteShotByIdRemovesShot() {
        val shot = shotEntity(id = "shot-1")
        dao.insertShot(shot)

        dao.deleteShotById("shot-1")

        assertNull(dao.getShotById("shot-1"))
    }

    @Test
    fun storedJsonMatchesCanonicalShotDraftJson() {
        val draft = sampleDraft(id = "shot-1")
        val json = ShotDraftJsonExporter.export(draft)
        val shot = ShotEntity(
            id = draft.id,
            json = json,
            createdAtEpochMillis = draft.createdAtEpochMs
        )

        dao.insertShot(shot)

        assertEquals(json, dao.getShotById(draft.id)?.json)
    }

    private fun shotEntity(
        id: String,
        createdAtEpochMillis: Long = 1_000L
    ): ShotEntity =
        ShotEntity(
            id = id,
            json = """{"schemaVersion":1,"shot":{"id":"$id"}}""",
            createdAtEpochMillis = createdAtEpochMillis
        )

    private fun sampleDraft(id: String): ShotDraft =
        ShotDraft(
            id = id,
            createdAtEpochMs = 1_000L,
            target = CaptureTarget(
                source = ShotSource.QUICK_SHOT,
                recipeId = null,
                beanId = null,
                doseG = 18.0,
                targetRatio = 2.0,
                targetYieldG = 36.0,
                targetTimeS = null
            ),
            timing = ShotTiming(
                startMode = StartMode.AUTO_WEIGHT,
                stopMode = StopMode.TARGET_YIELD,
                brewTimeMs = null,
                flowTimeMs = 25_000L,
                targetReachedAtMs = 24_000L,
                firstWeightDelayMs = null,
                postTargetRecordingMs = 1_500L
            ),
            result = ShotResult(
                actualYieldG = 37.25,
                postTargetDriftG = 1.25,
                averageFlowGPerS = null,
                maxFlowGPerS = null,
                sampleCount = 3
            ),
            samples = listOf(
                CapturedSample(
                    index = 0,
                    tMs = 0L,
                    weightGRaw = 0.0,
                    weightGFiltered = 0.0,
                    flowGPerS = null,
                    isOutlier = false,
                    source = SampleSource.SCALE
                ),
                CapturedSample(
                    index = 1,
                    tMs = 12_000L,
                    weightGRaw = 18.25,
                    weightGFiltered = 18.25,
                    flowGPerS = null,
                    isOutlier = false,
                    source = SampleSource.SCALE
                ),
                CapturedSample(
                    index = 2,
                    tMs = 25_000L,
                    weightGRaw = 37.25,
                    weightGFiltered = 37.25,
                    flowGPerS = null,
                    isOutlier = false,
                    source = SampleSource.SCALE
                )
            ),
            status = ShotStatus.COMPLETED,
            notes = null
        )
}
