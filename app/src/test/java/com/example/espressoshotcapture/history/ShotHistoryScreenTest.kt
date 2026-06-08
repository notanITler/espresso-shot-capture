package com.example.espressoshotcapture.history

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                    targetYieldLabel = "Target: 36.0 g"
                ),
                ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)
            )
        )

        composeTestRule.onNodeWithText("shot-1000").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Yield: 36.8 g  |  Flow time: 28 s  |  Target: 36.0 g"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("shot-2000").assertIsDisplayed()
    }

    @Test
    fun uiStateCanRenderHistoryItems() {
        composeTestRule.activity.setContent {
            ShotHistoryScreen(
                uiState = ShotHistoryUiState(
                    items = listOf(
                        ShotHistoryItem(id = "shot-3000", createdAtEpochMillis = 3_000L)
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("shot-3000").assertIsDisplayed()
    }

    @Test
    fun longHistoryOnlyShowsNewestFiveRows() {
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

        composeTestRule.onNodeWithText("shot-6").assertIsDisplayed()
        composeTestRule.onNodeWithText("shot-2").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("shot-1").assertCountEquals(0)
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
                        finalYieldLabel = "Yield: 36.8 g",
                        flowTimeLabel = "Flow time: 28 s",
                        targetYieldLabel = "Target: 36.0 g",
                        averageFlowLabel = "Average flow: 1.3 g/s",
                        targetReachedLabel = "Target reached: yes"
                    )
                }
            )
        }

        composeTestRule.onNodeWithText("shot-2000").performClick()

        composeTestRule.onNodeWithText("Selected Shot Detail").assertIsDisplayed()
        composeTestRule.onNodeWithText("id: shot-2000").assertIsDisplayed()
        composeTestRule.onNodeWithText("createdAtEpochMillis: 2000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yield: 36.8 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Flow time: 28 s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Average flow: 1.3 g/s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target: 36.0 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target reached: yes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Raw JSON / debug detail").assertIsDisplayed()
        composeTestRule.onNodeWithText(json).assertIsDisplayed()
    }

    private fun setHistoryContent(items: List<ShotHistoryItem>) {
        composeTestRule.activity.setContent {
            ShotHistoryScreen(items = items)
        }
    }
}
