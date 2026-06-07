package com.example.espressoshotcapture.capture

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.espressoshotcapture.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keyCaptureLabelsAreDisplayed() {
        composeTestRule.activity.setContent {
            CaptureScreen()
        }

        composeTestRule.onNodeWithText("Espresso Shot Capture").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scale: Not connected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start capture").assertIsDisplayed()
    }

    @Test
    fun defaultUiStateHasPlaceholderLabels() {
        val uiState = CaptureUiStateMapper.initialDisconnectedReady()

        composeTestRule.activity.setContent {
            CaptureScreen(uiState = uiState)
        }

        composeTestRule.onNodeWithText("Scale: Not connected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start capture").assertIsDisplayed()
    }
}
