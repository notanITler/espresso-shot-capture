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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun updateShotUserMetadataWritesFullMetadata() = runTest {
        repository.saveShotDraft(sampleDraft(id = "shot-update", createdAtEpochMs = 1_234L))
        val metadata = ShotUserMetadata(
            rating = 5,
            tasteDirection = TasteDirection.BALANCED,
            grindSetting = "8.10",
            beanName = "Ethiopia Guji",
            notes = "Sweet and clean"
        )

        val updated = repository.updateShotUserMetadata("shot-update", metadata)

        val entity = requireNotNull(dao.getShotById("shot-update"))
        assertTrue(updated)
        assertEquals(metadata, ShotEntityMapper.toUserMetadata(entity))
        assertEquals("8.10", entity.grindSetting)
    }

    @Test
    fun updateShotUserMetadataCanClearOptionalFields() = runTest {
        val fullMetadata = ShotUserMetadata(
            rating = 4,
            tasteDirection = TasteDirection.BITTER,
            grindSetting = "9.0",
            beanName = "Brazil Natural",
            notes = "Dry finish"
        )
        val partialMetadata = ShotUserMetadata(
            rating = 3,
            tasteDirection = null,
            grindSetting = null,
            beanName = "Brazil Natural",
            notes = null
        )
        repository.saveShotDraft(
            shotDraft = sampleDraft(id = "shot-clear", createdAtEpochMs = 1_234L),
            userMetadata = fullMetadata
        )

        val updated = repository.updateShotUserMetadata("shot-clear", partialMetadata)

        val entity = requireNotNull(dao.getShotById("shot-clear"))
        assertTrue(updated)
        assertEquals(partialMetadata, ShotEntityMapper.toUserMetadata(entity))
        assertNull(entity.tasteDirection)
        assertNull(entity.grindSetting)
        assertNull(entity.notes)
    }

    @Test
    fun updateShotUserMetadataRejectsInvalidMetadataAndPreservesStoredData() = runTest {
        val originalMetadata = ShotUserMetadata(
            rating = 4,
            tasteDirection = TasteDirection.SOUR,
            grindSetting = "8.10",
            beanName = "Kenya AA",
            notes = "Bright"
        )
        repository.saveShotDraft(
            shotDraft = sampleDraft(id = "shot-invalid-update", createdAtEpochMs = 1_234L),
            userMetadata = originalMetadata
        )

        assertThrows(IllegalArgumentException::class.java) {
            repository.updateShotUserMetadata(
                shotId = "shot-invalid-update",
                metadata = ShotUserMetadata(rating = 0)
            )
        }

        val entity = requireNotNull(dao.getShotById("shot-invalid-update"))
        assertEquals(originalMetadata, ShotEntityMapper.toUserMetadata(entity))
    }

    @Test
    fun updateShotUserMetadataDoesNotChangeRawJson() = runTest {
        val draft = sampleDraft(id = "shot-json", createdAtEpochMs = 1_234L)
        repository.saveShotDraft(draft)
        val before = requireNotNull(dao.getShotById("shot-json"))

        repository.updateShotUserMetadata(
            shotId = "shot-json",
            metadata = ShotUserMetadata(rating = 5, grindSetting = "8.10")
        )

        val after = requireNotNull(dao.getShotById("shot-json"))
        assertEquals(before.json, after.json)
    }

    @Test
    fun updateShotUserMetadataDoesNotChangeCreatedAtEpochMillis() = runTest {
        repository.saveShotDraft(sampleDraft(id = "shot-created", createdAtEpochMs = 9_876L))
        val before = requireNotNull(dao.getShotById("shot-created"))

        repository.updateShotUserMetadata(
            shotId = "shot-created",
            metadata = ShotUserMetadata(rating = 2, notes = "Needs work")
        )

        val after = requireNotNull(dao.getShotById("shot-created"))
        assertEquals(before.createdAtEpochMillis, after.createdAtEpochMillis)
    }

    @Test
    fun updateShotUserMetadataReturnsFalseForMissingShotId() = runTest {
        val updated = repository.updateShotUserMetadata(
            shotId = "missing-shot",
            metadata = ShotUserMetadata(rating = 5)
        )

        assertFalse(updated)
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
