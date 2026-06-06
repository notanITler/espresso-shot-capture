package com.example.espressoshotcapture.repository

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
import com.example.espressoshotcapture.persistence.AppDatabase
import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShotRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ShotDao
    private lateinit var repository: ShotRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
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
    fun saveShotStoresCanonicalJson() = runTest {
        val draft = sampleDraft(id = "shot-1")

        repository.saveShot(draft, createdAtEpochMillis = 10_000L)

        assertEquals(
            ShotDraftJsonExporter.export(draft),
            dao.getShotById("shot-1")?.json
        )
    }

    @Test
    fun saveShotStoresShotId() = runTest {
        val draft = sampleDraft(id = "shot-1")

        repository.saveShot(draft, createdAtEpochMillis = 10_000L)

        assertEquals("shot-1", dao.getShotById("shot-1")?.id)
    }

    @Test
    fun saveShotStoresCreatedAtEpochMillis() = runTest {
        val draft = sampleDraft(id = "shot-1")

        repository.saveShot(draft, createdAtEpochMillis = 10_000L)

        assertEquals(10_000L, dao.getShotById("shot-1")?.createdAtEpochMillis)
    }

    @Test
    fun getAllShotsDelegatesToDaoData() = runTest {
        val first = shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L)
        val second = shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L)
        dao.insertShot(second)
        dao.insertShot(first)

        assertEquals(listOf(first, second), repository.getAllShots())
    }

    @Test
    fun deleteShotByIdRemovesShot() = runTest {
        dao.insertShot(shotEntity(id = "shot-1"))

        repository.deleteShotById("shot-1")

        assertNull(repository.getShotById("shot-1"))
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
