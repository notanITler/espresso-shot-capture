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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.espressoshotcapture.capture.domain.TasteDirection
import com.example.espressoshotcapture.MainActivity
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
                    comparisonTitleLabel = "Delta Espresso Bar | Rating 4/5",
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

        composeTestRule.onNodeWithText("Delta Espresso Bar | Rating 4/5").assertIsDisplayed()
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
    }

    @Test
    fun longHistoryOnlyShowsNewestThreeRows() {
        setHistoryContent(
            items = listOf(
                ShotHistoryItem(id = "shot-6", createdAtEpochMillis = 6_000L, comparisonTitleLabel = "Bean 6"),
                ShotHistoryItem(id = "shot-5", createdAtEpochMillis = 5_000L, comparisonTitleLabel = "Bean 5"),
                ShotHistoryItem(id = "shot-4", createdAtEpochMillis = 4_000L, comparisonTitleLabel = "Bean 4"),
                ShotHistoryItem(id = "shot-3", createdAtEpochMillis = 3_000L, comparisonTitleLabel = "Bean 3"),
                ShotHistoryItem(id = "shot-2", createdAtEpochMillis = 2_000L, comparisonTitleLabel = "Bean 2"),
                ShotHistoryItem(id = "shot-1", createdAtEpochMillis = 1_000L, comparisonTitleLabel = "Bean 1")
            )
        )

        composeTestRule.onNodeWithText("Bean 6").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.HISTORY_LIST)
            .performScrollToNode(hasText("Bean 4"))
        composeTestRule.onNodeWithText("Bean 4").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Bean 3").assertCountEquals(0)
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
        composeTestRule.onAllNodesWithText("Source: Decent Scale").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Data status: Complete").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Yield: 36.8 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Flow time: 28 s").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Average flow: 1.3 g/s").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Weight readings: 4").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Dose: 18.0 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Target: 36.0 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Ratio: 1:2").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Target reached: yes").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("id: shot-2000").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Created: 2000").assertCountEquals(0)
        composeTestRule.onAllNodesWithText(ShotHistoryMapper.createdLabel(2_000L)).assertCountEquals(1)
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
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_RATING_INPUT)
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
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_RATING_INPUT)
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
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_RATING_INPUT)
            .performScrollTo()
            .assertTextEquals("4")
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
            .onNodeWithTag(ShotHistoryScreenTestTags.METADATA_RATING_INPUT)
            .performScrollTo()
            .assertTextEquals("")
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

    private fun setHistoryContent(items: List<ShotHistoryItem>) {
        setScrollableContent {
            ShotHistoryScreen(items = items)
        }
    }

    private fun selectShotWithMetadataEditor(
        detail: ShotHistoryDetail,
        metadataEditor: ShotUserMetadataEditorState
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
                }
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
