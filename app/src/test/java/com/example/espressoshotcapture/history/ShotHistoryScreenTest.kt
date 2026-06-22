package com.example.espressoshotcapture.history

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
                    finalYieldLabel = "Yield: 36.8 g",
                    flowTimeLabel = "Flow time: 28 s",
                    targetYieldLabel = "Target: 36.0 g",
                    sourceLabel = "Source: Fake/demo",
                    qualityLabel = "Quality: Complete",
                    sampleCountLabel = "Samples: 3",
                    doseLabel = "Dose: 18.0 g"
                ),
                ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)
            )
        )

        composeTestRule.onNodeWithText(
            "Source: Fake/demo  |  Yield: 36.8 g"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Flow time: 28 s  |  Samples: 3  |  Quality: Complete"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Created: 1000"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Source: --  |  Yield: --").assertIsDisplayed()
    }

    @Test
    fun uiStateCanRenderHistoryItems() {
        composeTestRule.activity.setContent {
            ShotHistoryScreen(
                uiState = ShotHistoryUiState(
                    items = listOf(
                        ShotHistoryItem(
                            id = "shot-3000",
                            createdAtEpochMillis = 3_000L,
                            sourceLabel = "Source: Decent Scale",
                            finalYieldLabel = "Yield: 37.1 g",
                            flowTimeLabel = "Flow time: 29 s",
                            sampleCountLabel = "Samples: 12",
                            qualityLabel = "Quality: Complete"
                        )
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("Source: Decent Scale  |  Yield: 37.1 g").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Flow time: 29 s  |  Samples: 12  |  Quality: Complete"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Created: 3000").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Raw JSON / debug detail").assertCountEquals(0)
    }

    @Test
    fun longHistoryOnlyShowsNewestThreeRows() {
        setHistoryContent(
            items = listOf(
                ShotHistoryItem(id = "shot-6", createdAtEpochMillis = 6_000L),
                ShotHistoryItem(id = "shot-5", createdAtEpochMillis = 5_000L),
                ShotHistoryItem(id = "shot-4", createdAtEpochMillis = 4_000L),
                ShotHistoryItem(id = "shot-3", createdAtEpochMillis = 3_000L),
                ShotHistoryItem(id = "shot-2", createdAtEpochMillis = 2_000L),
                ShotHistoryItem(id = "shot-1", createdAtEpochMillis = 1_000L)
            )
        )

        composeTestRule.onNodeWithText("Created: 6000").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ShotHistoryScreenTestTags.HISTORY_LIST)
            .performScrollToNode(hasText("Created: 4000"))
        composeTestRule.onNodeWithText("Created: 4000").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Created: 3000").assertCountEquals(0)
    }

    @Test
    fun clickingRowShowsShotDetail() {
        val selectedDetail = mutableStateOf<ShotHistoryDetail?>(null)
        val json = """{"schemaVersion":1,"shot":{"id":"shot-2000"}}"""
        val items = listOf(
            ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)
        )

        composeTestRule.activity.setContent {
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
                        qualityLabel = "Quality: Complete",
                        finalYieldLabel = "Yield: 36.8 g",
                        flowTimeLabel = "Flow time: 28 s",
                        targetYieldLabel = "Target: 36.0 g",
                        averageFlowLabel = "Average flow: 1.3 g/s",
                        sampleCountLabel = "Samples: 4",
                        doseLabel = "Dose: 18.0 g",
                        targetReachedLabel = "Target reached: yes"
                    )
                }
            )
        }

        composeTestRule.onNodeWithText("Created: 2000").performClick()

        composeTestRule.onNodeWithText("Selected Shot Detail").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.SELECTED_DETAIL).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("id: shot-2000").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("createdAtEpochMillis: 2000").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Source: Decent Scale").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Quality: Complete").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Yield: 36.8 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Flow time: 28 s").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Average flow: 1.3 g/s").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Samples: 4").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Dose: 18.0 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Target: 36.0 g").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Target reached: yes").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Raw JSON / debug detail").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(ShotHistoryScreenTestTags.RAW_JSON).assertCountEquals(1)
        composeTestRule.onAllNodesWithText(json).assertCountEquals(1)
    }

    private fun setHistoryContent(items: List<ShotHistoryItem>) {
        composeTestRule.activity.setContent {
            ShotHistoryScreen(items = items)
        }
    }
}
