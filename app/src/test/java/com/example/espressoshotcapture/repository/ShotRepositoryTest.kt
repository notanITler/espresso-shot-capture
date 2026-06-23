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
import com.example.espressoshotcapture.capture.domain.ShotUserMetadata
import com.example.espressoshotcapture.capture.domain.StartMode
import com.example.espressoshotcapture.capture.domain.StopMode
import com.example.espressoshotcapture.capture.domain.TasteDirection
import com.example.espressoshotcapture.persistence.EspressoShotDatabase
import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
    fun observingShotsEmitsNewestShotsFirst() = runTest {
        val older = shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L)
        val newer = shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L)

        repository.saveShot(older)
        repository.saveShot(newer)

        assertEquals(listOf(newer, older), repository.observeShots().first())
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

    @Test
    fun saveShotDraftStoresMappedEntity() = runTest {
        val draft = sampleDraft(id = "shot-1", createdAtEpochMs = 1_234L)

        repository.saveShotDraft(draft)

        assertEquals(
            ShotEntityMapper.fromShotDraft(draft),
            dao.getShotById("shot-1")
        )
    }

    @Test
    fun saveShotDraftWithoutMetadataStoresNullMetadata() = runTest {
        repository.saveShotDraft(sampleDraft(id = "shot-empty", createdAtEpochMs = 1_234L))

        val entity = requireNotNull(dao.getShotById("shot-empty"))
        assertEquals(ShotUserMetadata(), ShotEntityMapper.toUserMetadata(entity))
        assertNull(entity.rating)
        assertNull(entity.tasteDirection)
        assertNull(entity.grindSetting)
        assertNull(entity.beanName)
        assertNull(entity.notes)
    }

    @Test
    fun saveShotDraftPersistsFullMetadata() = runTest {
        val metadata = ShotUserMetadata(
            rating = 4,
            tasteDirection = TasteDirection.SOUR,
            grindSetting = "8.10",
            beanName = "Kenya AA",
            notes = "Increase yield next time"
        )

        repository.saveShotDraft(
            shotDraft = sampleDraft(id = "shot-full", createdAtEpochMs = 1_234L),
            userMetadata = metadata
        )

        val entity = requireNotNull(dao.getShotById("shot-full"))
        assertEquals(metadata, ShotEntityMapper.toUserMetadata(entity))
        assertEquals("8.10", entity.grindSetting)
    }

    @Test
    fun invalidMetadataIsRejectedBeforeInsert() = runTest {
        val invalidMetadata = ShotUserMetadata(rating = 6)

        assertThrows(IllegalArgumentException::class.java) {
            repository.saveShotDraft(
                shotDraft = sampleDraft(id = "shot-invalid", createdAtEpochMs = 1_234L),
                userMetadata = invalidMetadata
            )
        }
        assertNull(dao.getShotById("shot-invalid"))
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

    private fun sampleDraft(
        id: String,
        createdAtEpochMs: Long
    ): ShotDraft =
        ShotDraft(
            id = id,
            createdAtEpochMs = createdAtEpochMs,
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
                sampleCount = 1
            ),
            samples = listOf(
                CapturedSample(
                    index = 0,
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
