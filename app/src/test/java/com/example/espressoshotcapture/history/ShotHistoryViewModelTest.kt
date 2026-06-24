package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.capture.domain.ShotUserMetadata
import com.example.espressoshotcapture.capture.domain.TasteDirection
import com.example.espressoshotcapture.persistence.ShotDao
import com.example.espressoshotcapture.persistence.ShotEntity
import com.example.espressoshotcapture.repository.ShotRepository
import com.example.espressoshotcapture.repository.ShotEntityMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShotHistoryViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeShotDao
    private lateinit var viewModel: ShotHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeShotDao()
        viewModel = ShotHistoryViewModel(
            shotRepository = ShotRepository(dao),
            metadataWriteDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyRepositoryProducesEmptyUiState() = runTest(testDispatcher) {
        assertEquals(
            ShotHistoryUiState(items = emptyList()),
            viewModel.uiState.value
        )
    }

    @Test
    fun repositoryEmissionsShowNewestShotsFirstAndMapIdsAndCreatedAtEpochMillis() = runTest(testDispatcher) {
        dao.insertShot(shotEntity(id = "shot-3", createdAtEpochMillis = 3_000L))
        dao.insertShot(shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L))
        dao.insertShot(shotEntity(id = "shot-2", createdAtEpochMillis = 2_000L))

        val uiState = viewModel.uiState
            .first { state -> state.items.size == 3 }

        assertEquals(
            ShotHistoryUiState(
                items = listOf(
                    ShotHistoryItem(
                        id = "shot-3",
                        createdAtEpochMillis = 3_000L,
                        qualityLabel = "Data status: Missing target"
                    ),
                    ShotHistoryItem(
                        id = "shot-2",
                        createdAtEpochMillis = 2_000L,
                        qualityLabel = "Data status: Missing target"
                    ),
                    ShotHistoryItem(
                        id = "shot-1",
                        createdAtEpochMillis = 1_000L,
                        qualityLabel = "Data status: Missing target"
                    )
                )
            ),
            uiState
        )
    }

    @Test
    fun selectingShotShowsDetailFromRepositoryJson() = runTest(testDispatcher) {
        val json = """{"schemaVersion":1,"shot":{"id":"shot-2","status":"COMPLETED"}}"""
        dao.insertShot(shotEntity(id = "shot-1", createdAtEpochMillis = 1_000L))
        dao.insertShot(
            shotEntity(
                id = "shot-2",
                createdAtEpochMillis = 2_000L,
                json = json
            )
        )

        viewModel.selectShot("shot-2")

        val uiState = viewModel.uiState
            .first { state -> state.selectedShotDetail?.id == "shot-2" }

        assertEquals(
            ShotHistoryDetail(
                id = "shot-2",
                createdAtEpochMillis = 2_000L,
                json = json,
                qualityLabel = "Data status: Missing target"
            ),
            uiState.selectedShotDetail
        )
    }

    @Test
    fun selectingShotExposesExistingMetadata() = runTest(testDispatcher) {
        dao.insertShot(
            shotEntity(
                id = "shot-metadata",
                createdAtEpochMillis = 1_000L,
                rating = 4,
                tasteDirection = TasteDirection.BALANCED.name,
                grindSetting = "8.10",
                beanName = "Ethiopia Guji",
                notes = "Sweet"
            )
        )

        viewModel.selectShot("shot-metadata")

        val editor = viewModel.uiState
            .first { state -> state.metadataEditor?.shotId == "shot-metadata" }
            .metadataEditor

        assertEquals(
            ShotUserMetadataEditorState(
                shotId = "shot-metadata",
                ratingText = "4",
                tasteDirection = TasteDirection.BALANCED,
                grindSetting = "8.10",
                beanName = "Ethiopia Guji",
                notes = "Sweet"
            ),
            editor
        )
    }

    @Test
    fun updatingMetadataWritesThroughRepositoryUpdatePath() = runTest(testDispatcher) {
        dao.insertShot(shotEntity(id = "shot-update", createdAtEpochMillis = 1_000L))
        viewModel.selectShot("shot-update")
        viewModel.uiState.first { state -> state.metadataEditor?.shotId == "shot-update" }

        viewModel.updateMetadataRating("5")
        viewModel.updateMetadataTasteDirection(TasteDirection.SOUR)
        viewModel.updateMetadataGrindSetting("8.10")
        viewModel.updateMetadataBeanName("Kenya AA")
        viewModel.updateMetadataNotes("Bright")
        viewModel.saveShotUserMetadata()
        testDispatcher.scheduler.advanceUntilIdle()

        val entity = requireNotNull(dao.getShotById("shot-update"))
        assertEquals(
            ShotUserMetadata(
                rating = 5,
                tasteDirection = TasteDirection.SOUR,
                grindSetting = "8.10",
                beanName = "Kenya AA",
                notes = "Bright"
            ),
            ShotEntityMapper.toUserMetadata(entity)
        )
    }

    @Test
    fun savingEmptyMetadataDoesNotCrashAndStoresEmptyMetadata() = runTest(testDispatcher) {
        dao.insertShot(shotEntity(id = "shot-empty", createdAtEpochMillis = 1_000L))
        viewModel.selectShot("shot-empty")
        viewModel.uiState.first { state -> state.metadataEditor?.shotId == "shot-empty" }

        viewModel.saveShotUserMetadata()
        testDispatcher.scheduler.advanceUntilIdle()

        val entity = requireNotNull(dao.getShotById("shot-empty"))
        val editor = viewModel.uiState
            .first { state -> state.metadataEditor?.validationMessage == "Shot feedback saved" }
            .metadataEditor
        assertEquals(ShotUserMetadata(), ShotEntityMapper.toUserMetadata(entity))
        assertEquals("Shot feedback saved", editor?.validationMessage)
    }

    @Test
    fun invalidMetadataIsRejectedAndDoesNotChangeStoredData() = runTest(testDispatcher) {
        dao.insertShot(
            shotEntity(
                id = "shot-invalid",
                createdAtEpochMillis = 1_000L,
                rating = 3,
                grindSetting = "8.10"
            )
        )
        viewModel.selectShot("shot-invalid")
        viewModel.uiState.first { state -> state.metadataEditor?.shotId == "shot-invalid" }

        viewModel.updateMetadataGrindSetting("eight")
        viewModel.saveShotUserMetadata()
        testDispatcher.scheduler.advanceUntilIdle()

        val entity = requireNotNull(dao.getShotById("shot-invalid"))
        val editor = viewModel.uiState
            .first { state ->
                state.metadataEditor?.validationMessage == "Grind setting must be a decimal value"
            }
            .metadataEditor
        assertEquals("8.10", entity.grindSetting)
        assertEquals("Grind setting must be a decimal value", editor?.validationMessage)
    }

    @Test
    fun clearingMetadataWorks() = runTest(testDispatcher) {
        dao.insertShot(
            shotEntity(
                id = "shot-clear",
                createdAtEpochMillis = 1_000L,
                rating = 4,
                tasteDirection = TasteDirection.BITTER.name,
                grindSetting = "9.0",
                beanName = "Brazil",
                notes = "Dry"
            )
        )
        viewModel.selectShot("shot-clear")
        viewModel.uiState.first { state -> state.metadataEditor?.shotId == "shot-clear" }

        viewModel.clearShotUserMetadata()
        testDispatcher.scheduler.advanceUntilIdle()

        val entity = requireNotNull(dao.getShotById("shot-clear"))
        assertEquals(ShotUserMetadata(), ShotEntityMapper.toUserMetadata(entity))
        assertNull(entity.rating)
        assertNull(entity.tasteDirection)
        assertNull(entity.grindSetting)
        assertNull(entity.beanName)
        assertNull(entity.notes)
    }

    @Test
    fun saveMetadataFailureSurfacesUiErrorInsteadOfThrowing() = runTest(testDispatcher) {
        dao.insertShot(shotEntity(id = "shot-fail", createdAtEpochMillis = 1_000L))
        viewModel.selectShot("shot-fail")
        viewModel.uiState.first { state -> state.metadataEditor?.shotId == "shot-fail" }
        dao.shouldFailMetadataUpdate = true

        viewModel.updateMetadataBeanName("Kenya AA")
        viewModel.saveShotUserMetadata()
        testDispatcher.scheduler.advanceUntilIdle()

        val editor = viewModel.uiState
            .first { state ->
                state.metadataEditor?.validationMessage == "Could not save shot feedback"
            }
            .metadataEditor

        assertEquals("Could not save shot feedback", editor?.validationMessage)
    }

    @Test
    fun missingShotIdShowsUserFacingMessageInsteadOfCrashing() = runTest(testDispatcher) {
        dao.insertShot(shotEntity(id = "shot-missing", createdAtEpochMillis = 1_000L))
        viewModel.selectShot("shot-missing")
        viewModel.uiState.first { state -> state.metadataEditor?.shotId == "shot-missing" }
        dao.deleteShotById("shot-missing")

        viewModel.updateMetadataBeanName("Kenya AA")
        viewModel.saveShotUserMetadata()
        testDispatcher.scheduler.advanceUntilIdle()

        val editor = viewModel.uiState
            .first { state -> state.metadataEditor?.validationMessage == "Shot no longer exists" }
            .metadataEditor
        assertEquals("Shot no longer exists", editor?.validationMessage)
    }

    private fun shotEntity(
        id: String,
        createdAtEpochMillis: Long,
        json: String = """{"schemaVersion":1,"shot":{"id":"$id"}}""",
        rating: Int? = null,
        tasteDirection: String? = null,
        grindSetting: String? = null,
        beanName: String? = null,
        notes: String? = null
    ): ShotEntity =
        ShotEntity(
            id = id,
            json = json,
            createdAtEpochMillis = createdAtEpochMillis,
            rating = rating,
            tasteDirection = tasteDirection,
            grindSetting = grindSetting,
            beanName = beanName,
            notes = notes
        )
}

private class FakeShotDao : ShotDao {
    private val shots = mutableListOf<ShotEntity>()
    private val shotsFlow = MutableStateFlow<List<ShotEntity>>(emptyList())
    var shouldFailMetadataUpdate: Boolean = false

    override fun insertShot(entity: ShotEntity) {
        shots.removeAll { shot -> shot.id == entity.id }
        shots.add(entity)
        shotsFlow.value = orderedShots()
    }

    override fun observeShots(): Flow<List<ShotEntity>> = shotsFlow

    override fun getShotById(id: String): ShotEntity? =
        shots.firstOrNull { shot -> shot.id == id }

    override fun getAllShotsOnce(): List<ShotEntity> =
        orderedShots()

    override fun deleteShotById(id: String) {
        shots.removeAll { shot -> shot.id == id }
        shotsFlow.value = orderedShots()
    }

    override fun updateShotUserMetadata(
        id: String,
        rating: Int?,
        tasteDirection: String?,
        grindSetting: String?,
        beanName: String?,
        notes: String?
    ): Int {
        if (shouldFailMetadataUpdate) {
            error("Metadata update failed")
        }
        val index = shots.indexOfFirst { shot -> shot.id == id }
        if (index == -1) return 0

        shots[index] = shots[index].copy(
            rating = rating,
            tasteDirection = tasteDirection,
            grindSetting = grindSetting,
            beanName = beanName,
            notes = notes
        )
        shotsFlow.value = orderedShots()
        return 1
    }

    private fun orderedShots(): List<ShotEntity> =
        shots.sortedByDescending { shot -> shot.createdAtEpochMillis }
}
