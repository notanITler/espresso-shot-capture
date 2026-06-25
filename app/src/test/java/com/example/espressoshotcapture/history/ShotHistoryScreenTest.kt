package com.example.espressoshotcapture.history

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.espressoshotcapture.capture.domain.TasteDirection
import com.example.espressoshotcapture.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShotHistoryScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun emptyStateVisibleWhenListEmpty() {
        setHistoryContent(items = emptyList())

        composeTestRule
            .onNodeWithText("No saved shots")
            .assertIsDisplayed()
    }

    @Test
    fun rowsDisplayedWhenItemsExist() {
        setHistoryContent(
            items = listOf(
                ShotHistoryItem(
                    id = "shot-1000",
                    createdAtEpochMillis = 1_000L,
                    comparisonTitleLabel = "Delta Espresso Bar | ★★★★☆",
                    comparisonMetadataLabel = "Grind 8.10 | Balanced",
                    comparisonMetricsLabel = "18.0 g -> 36.8 g | 28 s | 1.3 g/s"
                ),
                ShotHistoryItem(
                    id = "shot-2000",
                    createdAtEpochMillis = 2_000L,
                    comparisonMetricsLabel = "18.0 g -> 37.1 g | 29 s | 1.3 g/s"
                )
            )
        )

        composeTestRule.onNodeWithText("Delta Espresso Bar | ★★★★☆").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grind 8.10 | Balanced").assertIsDisplayed()
        composeTestRule.onNodeWithText("18.0 g -> 36.8 g | 28 s | 1.3 g/s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unassigned bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("18.0 g -> 37.1 g | 29 s | 1.3 g/s").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("shot-1000").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Created: 1000").assertCountEquals(0)
    }

    @Test
    fun uiStateCanRenderHistoryItems() {
        setScrollableContent {
            ShotHistoryScreen(
                uiState = ShotHistoryUiState(
                    items = listOf(
                        ShotHistoryItem(
                            id = "shot-3000",
                            createdAtEpochMillis = 3_000L,
                            comparisonTitleLabel = "Unassigned bean",
                            comparisonMetricsLabel = "18.0 g -> 37.1 g | 29 s | 1.3 g/s"
                        )
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("Unassigned bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("18.0 g -> 37.1 g | 29 s | 1.3 g/s").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(ShotHistoryMapper.createdLabel(3_000L)).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Raw JSON / debug detail").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.SELECTED_DETAIL).assertCountEquals(0)
    }

    @Test
    fun historyPreviewShowsOnlyThreeRowsByDefaultAndViewAllExpands() {
        var selectedShotId: String? = null
        setHistoryContent(
            items = listOf(
                ShotHistoryItem(id = "shot-6", createdAtEpochMillis = 6_000L, comparisonTitleLabel = "Bean 6"),
                ShotHistoryItem(id = "shot-5", createdAtEpochMillis = 5_000L, comparisonTitleLabel = "Bean 5"),
                ShotHistoryItem(id = "shot-4", createdAtEpochMillis = 4_000L, comparisonTitleLabel = "Bean 4"),
                ShotHistoryItem(id = "shot-3", createdAtEpochMillis = 3_000L, comparisonTitleLabel = "Bean 3"),
                ShotHistoryItem(id = "shot-2", createdAtEpochMillis = 2_000L, comparisonTitleLabel = "Bean 2"),
                ShotHistoryItem(id = "shot-1", createdAtEpochMillis = 1_000L, comparisonTitleLabel = "Bean 1")
            ),
            onShotSelected = { id -> selectedShotId = id }
        )

        composeTestRule.onNodeWithText("Bean 6").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bean 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bean 4").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Bean 3").assertCountEquals(0)
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.HISTORY_EXPAND_ACTION)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("View all history").assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.HISTORY_EXPAND_ACTION)
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-1"))
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals("shot-1", selectedShotId)
        }
        composeTestRule.onNodeWithText("Show fewer").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun beanFilterShowsOnlyMatchingBeanShots() {
        val selectedFilter = mutableStateOf(ShotHistoryBeanFilterKeys.ALL)
        val allItems = comparisonItems()
        setScrollableContent {
            ShotHistoryScreen(
                items = allItems.filterForTest(selectedFilter.value),
                beanFilterOptions = beanFilterOptionsForTest(selectedFilter.value),
                onBeanFilterSelected = { key -> selectedFilter.value = key }
            )
        }

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.beanFilterOption(ShotHistoryBeanFilterKeys.bean("delta espresso bar")))
            .performClick()

        composeTestRule.onNodeWithText("Delta Espresso Bar | ★★★★☆").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Hannoversche Kaffeemanufaktur | ★★★★★").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Unassigned bean").assertCountEquals(0)
    }

    @Test
    fun beanFilterShowsMoreThanThreeMatchingShots() {
        val selectedFilter = mutableStateOf(ShotHistoryBeanFilterKeys.ALL)
        val allItems = manyComparisonItems()
        setScrollableContent {
            ShotHistoryScreen(
                items = allItems.filterForTest(selectedFilter.value),
                beanFilterOptions = beanFilterOptionsForTest(selectedFilter.value),
                onBeanFilterSelected = { key -> selectedFilter.value = key }
            )
        }

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.beanFilterOption(ShotHistoryBeanFilterKeys.bean("delta espresso bar")))
            .performClick()

        composeTestRule.onNodeWithText("Delta Espresso Bar | ★★★★★").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delta Espresso Bar | ★★★★☆").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delta Espresso Bar | ★★★☆☆").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Delta Espresso Bar | ★★☆☆☆").assertCountEquals(0)

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.HISTORY_EXPAND_ACTION)
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithTag(ShotHistoryScreenTestTags.historyRow("shot-delta-1"))
            .assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Delta Espresso Bar | ★☆☆☆☆").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Hannoversche Kaffeemanufaktur | ★★★★☆").assertCountEquals(0)
    }

    @Test
    fun unassignedFilterShowsShotsWithoutBeanName() {
        val selectedFilter = mutableStateOf(ShotHistoryBeanFilterKeys.ALL)
        val allItems = comparisonItems()
        setScrollableContent {
            ShotHistoryScreen(
                items = allItems.filterForTest(selectedFilter.value),
                beanFilterOptions = beanFilterOptionsForTest(selectedFilter.value),
                onBeanFilterSelected = { key -> selectedFilter.value = key }
            )
        }

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.beanFilterOption(ShotHistoryBeanFilterKeys.UNASSIGNED))
            .performClick()

        composeTestRule.onNodeWithText("Unassigned bean").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Delta Espresso Bar | ★★★★☆").assertCountEquals(0)
    }

    @Test
    fun unassignedFilterShowsMoreThanThreeMatchingShots() {
        val selectedFilter = mutableStateOf(ShotHistoryBeanFilterKeys.ALL)
        val allItems = manyComparisonItems()
        setScrollableContent {
            ShotHistoryScreen(
                items = allItems.filterForTest(selectedFilter.value),
                beanFilterOptions = beanFilterOptionsForTest(selectedFilter.value),
                onBeanFilterSelected = { key -> selectedFilter.value = key }
            )
        }

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.beanFilterOption(ShotHistoryBeanFilterKeys.UNASSIGNED))
            .performClick()

        composeTestRule.onNodeWithText("Unassigned bean 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unassigned bean 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unassigned bean 2").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Unassigned bean 1").assertCountEquals(0)

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.HISTORY_EXPAND_ACTION)
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithTag(ShotHistoryScreenTestTags.historyRow("shot-unassigned-1"))
            .assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Unassigned bean 1").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Delta Espresso Bar | ★★★★★").assertCountEquals(0)
    }

    @Test
    fun selectingAllRestoresFullHistory() {
        val selectedFilter = mutableStateOf(ShotHistoryBeanFilterKeys.bean("delta espresso bar"))
        val allItems = comparisonItems()
        setScrollableContent {
            ShotHistoryScreen(
                items = allItems.filterForTest(selectedFilter.value),
                beanFilterOptions = beanFilterOptionsForTest(selectedFilter.value),
                onBeanFilterSelected = { key -> selectedFilter.value = key }
            )
        }

        composeTestRule.onNodeWithText("Delta Espresso Bar | ★★★★☆").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.beanFilterOption(ShotHistoryBeanFilterKeys.ALL))
            .performClick()

        composeTestRule.onNodeWithText("Delta Espresso Bar | ★★★★☆").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-hannover"))
            .performScrollTo()
        composeTestRule.onNodeWithText("Hannoversche Kaffeemanufaktur | ★★★★★").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-unassigned"))
            .performScrollTo()
        composeTestRule.onNodeWithText("Unassigned bean").assertIsDisplayed()
    }

    @Test
    fun metadataEditorAppearsAfterFilteringAndSelectingShot() {
        val selectedFilter = mutableStateOf(ShotHistoryBeanFilterKeys.ALL)
        val selectedDetail = mutableStateOf<ShotHistoryDetail?>(null)
        val selectedEditor = mutableStateOf<ShotUserMetadataEditorState?>(null)
        val allItems = comparisonItems()
        setScrollableContent {
            ShotHistoryScreen(
                items = allItems.filterForTest(selectedFilter.value),
                beanFilterOptions = beanFilterOptionsForTest(selectedFilter.value),
                selectedShotDetail = selectedDetail.value,
                metadataEditor = selectedEditor.value,
                onBeanFilterSelected = { key ->
                    selectedFilter.value = key
                    selectedDetail.value = null
                    selectedEditor.value = null
                },
                onShotSelected = { id ->
                    selectedDetail.value = shotDetail().copy(id = id)
                    selectedEditor.value = ShotUserMetadataEditorState(shotId = id)
                }
            )
        }

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.beanFilterOption(ShotHistoryBeanFilterKeys.bean("delta espresso bar")))
            .performClick()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-delta"))
            .performClick()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_EDITOR)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun clickingRowShowsShotDetail() {
        val selectedDetail = mutableStateOf<ShotHistoryDetail?>(null)
        val json = """{"schemaVersion":1,"shot":{"id":"shot-2000"}}"""
        val items = listOf(
            ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)
        )

        setScrollableContent {
            ShotHistoryScreen(
                items = items,
                selectedShotDetail = selectedDetail.value,
                onShotSelected = { id ->
                    val item = items.first { historyItem -> historyItem.id == id }
                    selectedDetail.value = ShotHistoryDetail(
                        id = item.id,
                        createdAtEpochMillis = item.createdAtEpochMillis,
                        json = json,
                        sourceLabel = "Source: Decent Scale",
                        qualityLabel = "Data status: Complete",
                        finalYieldLabel = "Yield: 36.8 g",
                        flowTimeLabel = "Flow time: 28 s",
                        targetYieldLabel = "Target: 36.0 g",
                        ratioLabel = "Ratio: 1:2",
                        averageFlowLabel = "Average flow: 1.3 g/s",
                        sampleCountLabel = "Weight readings: 4",
                        doseLabel = "Dose: 18.0 g",
                        targetReachedLabel = "Target reached: yes"
                    )
                }
            )
        }

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-2000"))
            .performClick()

        composeTestRule.onNodeWithText("Selected Shot Detail").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.SELECTED_DETAIL).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.DETAIL_REPORT).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.DETAIL_MAIN_SUMMARY).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.DETAIL_TIMING_TARGET).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.DETAIL_DATA_CONFIDENCE).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Source").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Decent Scale").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Data status").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Complete").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Yield").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("36.8 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Flow time").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("28 s").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Average flow").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("1.3 g/s").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Weight readings").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("4").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Dose").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("18.0 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Target").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("36.0 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Ratio").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("1:2").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Target reached").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("yes").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Dose: 18.0 g").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Yield: 36.8 g").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Average flow: 1.3 g/s").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("id: shot-2000").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Created: 2000").assertCountEquals(0)
        composeTestRule.onAllNodesWithText(ShotHistoryMapper.createdLabel(2_000L)).assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText(ShotHistoryMapper.createdLabel(2_000L).substringAfter(": "))
            .assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.RAW_JSON_TOGGLE).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.RAW_JSON).assertCountEquals(0)
        composeTestRule.onAllNodesWithText(json).assertCountEquals(0)
    }

    @Test
    fun metadataEditorAppearsInSelectedShotDetail() {
        selectShotWithMetadataEditor(
            detail = shotDetail(),
            metadataEditor = ShotUserMetadataEditorState(shotId = "shot-2000")
        )

        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.METADATA_EDITOR).assertCountEquals(1)
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_EDITOR)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_RATING_STARS)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_TASTE_SOUR)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_TASTE_BALANCED)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_TASTE_BITTER)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_GRIND_INPUT)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_BEAN_INPUT)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_NOTES_INPUT)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun emptyMetadataRendersEmptyOptionalFields() {
        selectShotWithMetadataEditor(
            detail = shotDetail(),
            metadataEditor = ShotUserMetadataEditorState(shotId = "shot-2000")
        )

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_RATING_STARS)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_GRIND_INPUT)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_BEAN_INPUT)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_NOTES_INPUT)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun existingMetadataPrefillsFields() {
        selectShotWithMetadataEditor(
            detail = shotDetail(),
            metadataEditor = ShotUserMetadataEditorState(
                shotId = "shot-2000",
                ratingText = "4",
                tasteDirection = TasteDirection.BALANCED,
                grindSetting = "8.10",
                beanName = "Ethiopia Guji",
                notes = "Sweet and clear"
            )
        )

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(4))
            .performScrollTo()
            .assertTextEquals("★")
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(5))
            .performScrollTo()
            .assertTextEquals("☆")
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_GRIND_INPUT)
            .performScrollTo()
            .assertTextEquals("8.10")
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_BEAN_INPUT)
            .performScrollTo()
            .assertTextEquals("Ethiopia Guji")
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_NOTES_INPUT)
            .performScrollTo()
            .assertTextEquals("Sweet and clear")
    }

    @Test
    fun emptyRatingRendersEmptyStarsCleanly() {
        selectShotWithMetadataEditor(
            detail = shotDetail(),
            metadataEditor = ShotUserMetadataEditorState(shotId = "shot-2000")
        )

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(1))
            .performScrollTo()
            .assertTextEquals("☆")
    }

    @Test
    fun tappingStarsUpdatesAndClearsRating() {
        val editor = mutableStateOf(ShotUserMetadataEditorState(shotId = "shot-2000"))
        val selectedDetail = mutableStateOf<ShotHistoryDetail?>(null)
        val selectedEditor = mutableStateOf<ShotUserMetadataEditorState?>(null)
        setScrollableContent {
            ShotHistoryScreen(
                items = listOf(ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)),
                selectedShotDetail = selectedDetail.value,
                metadataEditor = selectedEditor.value,
                onShotSelected = {
                    selectedDetail.value = shotDetail()
                    selectedEditor.value = editor.value
                },
                onMetadataRatingChange = { value ->
                    editor.value = editor.value.copy(ratingText = value)
                    selectedEditor.value = editor.value
                }
            )
        }
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-2000"))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(4))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(4))
            .performScrollTo()
            .assertTextEquals("★")
        composeTestRule.runOnIdle {
            assertEquals("4", editor.value.ratingText)
        }

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(2))
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals("2", editor.value.ratingText)
        }

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(2))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(1))
            .performScrollTo()
            .assertTextEquals("☆")
        composeTestRule.runOnIdle {
            assertEquals("", editor.value.ratingText)
        }
    }

    @Test
    fun beanSuggestionsFillBeanNameAndManualTypingStillWorks() {
        val editor = mutableStateOf(ShotUserMetadataEditorState(shotId = "shot-2000"))
        val selectedDetail = mutableStateOf<ShotHistoryDetail?>(null)
        val selectedEditor = mutableStateOf<ShotUserMetadataEditorState?>(null)
        setScrollableContent {
            ShotHistoryScreen(
                items = listOf(ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)),
                selectedShotDetail = selectedDetail.value,
                metadataEditor = selectedEditor.value,
                beanSuggestions = listOf("Delta Espresso Bar", "Hannoversche Kaffeemanufaktur"),
                onShotSelected = {
                    selectedDetail.value = shotDetail()
                    selectedEditor.value = editor.value
                },
                onMetadataBeanNameChange = { value ->
                    editor.value = editor.value.copy(beanName = value)
                    selectedEditor.value = editor.value
                }
            )
        }
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-2000"))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.BEAN_SUGGESTIONS)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.beanSuggestion("Delta Espresso Bar"))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_BEAN_INPUT)
            .performScrollTo()
            .assertTextEquals("Delta Espresso Bar")

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_BEAN_INPUT)
            .performTextClearance()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_BEAN_INPUT)
            .performTextInput("New Bean")
        composeTestRule.runOnIdle {
            assertEquals("New Bean", editor.value.beanName)
        }
    }

    @Test
    fun saveUpdatesDisplayedMetadata() {
        val editor = mutableStateOf(ShotUserMetadataEditorState(shotId = "shot-2000"))
        val selectedDetail = mutableStateOf<ShotHistoryDetail?>(null)
        val selectedEditor = mutableStateOf<ShotUserMetadataEditorState?>(null)
        setScrollableContent {
            ShotHistoryScreen(
                items = listOf(ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)),
                selectedShotDetail = selectedDetail.value,
                metadataEditor = selectedEditor.value,
                onShotSelected = {
                    selectedDetail.value = shotDetail()
                    selectedEditor.value = editor.value
                },
                onMetadataGrindSettingChange = { value ->
                    editor.value = editor.value.copy(grindSetting = value)
                    selectedEditor.value = editor.value
                },
                onMetadataSave = {
                    editor.value = editor.value.copy(validationMessage = "Shot feedback saved")
                    selectedEditor.value = editor.value
                }
            )
        }
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-2000"))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_GRIND_INPUT)
            .performScrollTo()
            .performTextInput("8.10")
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_SAVE_ACTION)
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_GRIND_INPUT)
            .performScrollTo()
            .assertTextEquals("8.10")
        composeTestRule.onNodeWithText("Shot feedback saved").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun invalidMetadataShowsValidationMessage() {
        selectShotWithMetadataEditor(
            detail = shotDetail(),
            metadataEditor = ShotUserMetadataEditorState(
                shotId = "shot-2000",
                grindSetting = "eight",
                validationMessage = "Grind setting must be a decimal value"
            )
        )

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_VALIDATION_MESSAGE)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Grind setting must be a decimal value")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun clearActionClearsMetadataFields() {
        val editor = mutableStateOf(
            ShotUserMetadataEditorState(
                shotId = "shot-2000",
                ratingText = "5",
                grindSetting = "8.10",
                beanName = "Kenya AA",
                notes = "Bright"
            )
        )
        val selectedDetail = mutableStateOf<ShotHistoryDetail?>(null)
        val selectedEditor = mutableStateOf<ShotUserMetadataEditorState?>(null)
        setScrollableContent {
            ShotHistoryScreen(
                items = listOf(ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)),
                selectedShotDetail = selectedDetail.value,
                metadataEditor = selectedEditor.value,
                onShotSelected = {
                    selectedDetail.value = shotDetail()
                    selectedEditor.value = editor.value
                },
                onMetadataClear = {
                    editor.value = ShotUserMetadataEditorState(
                        shotId = "shot-2000",
                        validationMessage = "Shot feedback cleared"
                    )
                    selectedEditor.value = editor.value
                }
            )
        }
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow("shot-2000"))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_CLEAR_ACTION)
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText("Shot feedback cleared").performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.metadataRatingStar(1))
            .performScrollTo()
            .assertTextEquals("☆")
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_GRIND_INPUT)
            .performScrollTo()
            .assertTextEquals("")
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_BEAN_INPUT)
            .performScrollTo()
            .assertTextEquals("")
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_NOTES_INPUT)
            .performScrollTo()
            .assertTextEquals("")
    }

    @Test
    fun deleteSelectedShotRequiresConfirmation() {
        var deleteCount = 0
        selectShotWithMetadataEditor(
            detail = shotDetail(),
            metadataEditor = ShotUserMetadataEditorState(shotId = "shot-2000"),
            onDeleteSelectedShot = { deleteCount += 1 }
        )

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.DELETE_SELECTED_ACTION)
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals(0, deleteCount)
        }
        composeTestRule.onNodeWithText("Confirm delete shot").performScrollTo().assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.DELETE_SELECTED_ACTION)
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals(1, deleteCount)
        }
    }

    @Test
    fun purgeHistoryRequiresConfirmation() {
        var purgeCount = 0
        setHistoryContent(
            items = listOf(
                ShotHistoryItem(id = "shot-1", createdAtEpochMillis = 1_000L)
            ),
            onPurgeHistory = { purgeCount += 1 }
        )

        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.PURGE_HISTORY_ACTION).assertCountEquals(0)
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.DANGER_ZONE_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.PURGE_HISTORY_ACTION)
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals(0, purgeCount)
        }
        composeTestRule.onNodeWithText("Confirm purge shot history").performScrollTo().assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.PURGE_HISTORY_ACTION)
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals(1, purgeCount)
        }
    }

    private fun setHistoryContent(
        items: List<ShotHistoryItem>,
        onShotSelected: (String) -> Unit = {},
        onPurgeHistory: () -> Unit = {}
    ) {
        setScrollableContent {
            ShotHistoryScreen(
                items = items,
                onShotSelected = onShotSelected,
                onPurgeHistory = onPurgeHistory
            )
        }
    }

    private fun comparisonItems(): List<ShotHistoryItem> =
        listOf(
            ShotHistoryItem(
                id = "shot-delta",
                createdAtEpochMillis = 3_000L,
                comparisonTitleLabel = "Delta Espresso Bar | ★★★★☆",
                comparisonMetadataLabel = "Grind 8.10 | Balanced",
                comparisonMetricsLabel = "18.0 g -> 36.5 g | 27.8 s | 1.3 g/s"
            ),
            ShotHistoryItem(
                id = "shot-hannover",
                createdAtEpochMillis = 2_000L,
                comparisonTitleLabel = "Hannoversche Kaffeemanufaktur | ★★★★★",
                comparisonMetadataLabel = "Grind 9.0 | Sour",
                comparisonMetricsLabel = "18.0 g -> 38.2 g | 29 s | 1.3 g/s"
            ),
            ShotHistoryItem(
                id = "shot-unassigned",
                createdAtEpochMillis = 1_000L,
                comparisonTitleLabel = "Unassigned bean",
                comparisonMetricsLabel = "18.0 g -> 35.8 g | 26 s | 1.4 g/s"
            )
        )

    private fun manyComparisonItems(): List<ShotHistoryItem> =
        listOf(
            ShotHistoryItem(
                id = "shot-delta-5",
                createdAtEpochMillis = 9_000L,
                comparisonTitleLabel = "Delta Espresso Bar | ★★★★★",
                comparisonMetadataLabel = "Grind 8.5 | Balanced",
                comparisonMetricsLabel = "18.0 g -> 36.5 g | 27.8 s | 1.3 g/s"
            ),
            ShotHistoryItem(
                id = "shot-unassigned-4",
                createdAtEpochMillis = 8_000L,
                comparisonTitleLabel = "Unassigned bean 4",
                comparisonMetricsLabel = "18.0 g -> 35.8 g | 26 s | 1.4 g/s"
            ),
            ShotHistoryItem(
                id = "shot-delta-4",
                createdAtEpochMillis = 7_000L,
                comparisonTitleLabel = "Delta Espresso Bar | ★★★★☆",
                comparisonMetadataLabel = "Grind 8.4 | Balanced",
                comparisonMetricsLabel = "18.0 g -> 36.4 g | 27.4 s | 1.3 g/s"
            ),
            ShotHistoryItem(
                id = "shot-unassigned-3",
                createdAtEpochMillis = 6_000L,
                comparisonTitleLabel = "Unassigned bean 3",
                comparisonMetricsLabel = "18.0 g -> 35.3 g | 26 s | 1.4 g/s"
            ),
            ShotHistoryItem(
                id = "shot-delta-3",
                createdAtEpochMillis = 5_000L,
                comparisonTitleLabel = "Delta Espresso Bar | ★★★☆☆",
                comparisonMetadataLabel = "Grind 8.3 | Bitter",
                comparisonMetricsLabel = "18.0 g -> 36.3 g | 27.3 s | 1.3 g/s"
            ),
            ShotHistoryItem(
                id = "shot-unassigned-2",
                createdAtEpochMillis = 4_000L,
                comparisonTitleLabel = "Unassigned bean 2",
                comparisonMetricsLabel = "18.0 g -> 35.2 g | 26 s | 1.4 g/s"
            ),
            ShotHistoryItem(
                id = "shot-delta-2",
                createdAtEpochMillis = 3_000L,
                comparisonTitleLabel = "Delta Espresso Bar | ★★☆☆☆",
                comparisonMetadataLabel = "Grind 8.2 | Sour",
                comparisonMetricsLabel = "18.0 g -> 36.2 g | 27.2 s | 1.3 g/s"
            ),
            ShotHistoryItem(
                id = "shot-unassigned-1",
                createdAtEpochMillis = 2_000L,
                comparisonTitleLabel = "Unassigned bean 1",
                comparisonMetricsLabel = "18.0 g -> 35.1 g | 26 s | 1.4 g/s"
            ),
            ShotHistoryItem(
                id = "shot-delta-1",
                createdAtEpochMillis = 1_000L,
                comparisonTitleLabel = "Delta Espresso Bar | ★☆☆☆☆",
                comparisonMetadataLabel = "Grind 8.1 | Sour",
                comparisonMetricsLabel = "18.0 g -> 36.1 g | 27.1 s | 1.3 g/s"
            ),
            ShotHistoryItem(
                id = "shot-hannover",
                createdAtEpochMillis = 500L,
                comparisonTitleLabel = "Hannoversche Kaffeemanufaktur | ★★★★☆",
                comparisonMetadataLabel = "Grind 9.0 | Balanced",
                comparisonMetricsLabel = "18.0 g -> 38.2 g | 29 s | 1.3 g/s"
            )
        )

    private fun List<ShotHistoryItem>.filterForTest(filterKey: String): List<ShotHistoryItem> =
        when (filterKey) {
            ShotHistoryBeanFilterKeys.ALL -> this
            ShotHistoryBeanFilterKeys.UNASSIGNED ->
                filter { item -> item.id == "shot-unassigned" || item.id.startsWith("shot-unassigned-") }
            ShotHistoryBeanFilterKeys.bean("delta espresso bar") ->
                filter { item -> item.id == "shot-delta" || item.id.startsWith("shot-delta-") }
            ShotHistoryBeanFilterKeys.bean("hannoversche kaffeemanufaktur") ->
                filter { item -> item.id == "shot-hannover" }
            else -> emptyList()
        }

    private fun beanFilterOptionsForTest(selectedKey: String): List<ShotHistoryBeanFilterOption> =
        listOf(
            ShotHistoryBeanFilterOption(
                key = ShotHistoryBeanFilterKeys.ALL,
                label = "All shots",
                isSelected = selectedKey == ShotHistoryBeanFilterKeys.ALL
            ),
            ShotHistoryBeanFilterOption(
                key = ShotHistoryBeanFilterKeys.UNASSIGNED,
                label = "Unassigned",
                isSelected = selectedKey == ShotHistoryBeanFilterKeys.UNASSIGNED
            ),
            ShotHistoryBeanFilterOption(
                key = ShotHistoryBeanFilterKeys.bean("delta espresso bar"),
                label = "Delta Espresso Bar",
                isSelected = selectedKey == ShotHistoryBeanFilterKeys.bean("delta espresso bar")
            ),
            ShotHistoryBeanFilterOption(
                key = ShotHistoryBeanFilterKeys.bean("hannoversche kaffeemanufaktur"),
                label = "Hannoversche Kaffeemanufaktur",
                isSelected = selectedKey == ShotHistoryBeanFilterKeys.bean("hannoversche kaffeemanufaktur")
            )
        )

    private fun selectShotWithMetadataEditor(
        detail: ShotHistoryDetail,
        metadataEditor: ShotUserMetadataEditorState,
        onDeleteSelectedShot: () -> Unit = {}
    ) {
        val selectedDetail = mutableStateOf<ShotHistoryDetail?>(null)
        val selectedEditor = mutableStateOf<ShotUserMetadataEditorState?>(null)
        setScrollableContent {
            ShotHistoryScreen(
                items = listOf(ShotHistoryItem(id = detail.id, createdAtEpochMillis = detail.createdAtEpochMillis)),
                selectedShotDetail = selectedDetail.value,
                metadataEditor = selectedEditor.value,
                onShotSelected = {
                    selectedDetail.value = detail
                    selectedEditor.value = metadataEditor
                },
                onDeleteSelectedShot = onDeleteSelectedShot
            )
        }
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.historyRow(detail.id))
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_EDITOR)
            .performScrollTo()
    }

    private fun setScrollableContent(content: @Composable () -> Unit) {
        composeTestRule.activity.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                content()
            }
        }
    }

    private fun shotDetail(): ShotHistoryDetail =
        ShotHistoryDetail(
            id = "shot-2000",
            createdAtEpochMillis = 2_000L,
            json = """{"schemaVersion":1,"shot":{"id":"shot-2000"}}""",
            sourceLabel = "Source: Decent Scale",
            qualityLabel = "Data status: Complete",
            finalYieldLabel = "Yield: 36.8 g",
            flowTimeLabel = "Flow time: 28 s",
            targetYieldLabel = "Target: 36.0 g",
            ratioLabel = "Ratio: 1:2",
            averageFlowLabel = "Average flow: 1.3 g/s",
            sampleCountLabel = "Weight readings: 4",
            doseLabel = "Dose: 18.0 g",
            targetReachedLabel = "Target reached: yes"
        )
}

