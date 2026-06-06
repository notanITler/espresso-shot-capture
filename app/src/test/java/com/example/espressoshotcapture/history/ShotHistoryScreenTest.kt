package com.example.espressoshotcapture.history

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                ShotHistoryItem(id = "shot-1000", createdAtEpochMillis = 1_000L),
                ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)
            )
        )

        composeTestRule.onNodeWithText("shot-1000").assertIsDisplayed()
        composeTestRule.onNodeWithText("1000").assertIsDisplayed()
        composeTestRule.onNodeWithText("shot-2000").assertIsDisplayed()
        composeTestRule.onNodeWithText("2000").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("3000").assertIsDisplayed()
    }

    @Test
    fun debugActionCanBeClickedWhenProvided() {
        var clickCount = 0
        composeTestRule.activity.setContent {
            ShotHistoryScreen(
                items = emptyList(),
                onAddTestShot = { clickCount += 1 }
            )
        }

        composeTestRule.onNodeWithText("Add test shot").performClick()

        assertEquals(1, clickCount)
    }

    private fun setHistoryContent(items: List<ShotHistoryItem>) {
        composeTestRule.activity.setContent {
            ShotHistoryScreen(items = items)
        }
    }
}
