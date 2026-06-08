package com.example.espressoshotcapture.capture

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.espressoshotcapture.MainActivity
import org.junit.Assert.assertTrue
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
        composeTestRule.onNodeWithText("Dose: 18.0 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target yield: 36.0 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ratio: 1:2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Progress: 0.0 / 36.0 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target not reached").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Dose: 18.0 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target yield: 36.0 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ratio: 1:2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Progress: 0.0 / 36.0 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target not reached").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start capture").assertIsDisplayed()
    }

    @Test
    fun primaryActionInvokesCallback() {
        var clicked = false

        composeTestRule.activity.setContent {
            CaptureScreen(
                uiState = CaptureUiStateMapper.initialDisconnectedReady(),
                onPrimaryAction = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Start capture").performClick()

        assertTrue(clicked)
    }

    @Test
    fun stopAndSaveIsShownInRecording() {
        composeTestRule.activity.setContent {
            CaptureScreen(uiState = CaptureUiStateMapper.recording())
        }

        composeTestRule.onNodeWithText("Recording").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop & save").assertIsDisplayed()
    }

    @Test
    fun recordingValuesAreDisplayedWhileRecording() {
        composeTestRule.activity.setContent {
            CaptureScreen(uiState = CaptureUiStateMapper.recording())
        }

        composeTestRule.onNodeWithText("Weight: 0.0 g", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Progress: 0.0 / 36.0 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target not reached").assertIsDisplayed()
        composeTestRule.onNodeWithText("Flow time: 0 s", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Average flow: 0.0 g/s", substring = true).assertIsDisplayed()
    }

    @Test
    fun targetReachedIsDisplayedWhenProvidedByUiState() {
        composeTestRule.activity.setContent {
            CaptureScreen(
                uiState = CaptureUiStateMapper.recording().copy(
                    currentWeightLabel = "Weight: 36.0 g",
                    progressLabel = "Progress: 36.0 / 36.0 g",
                    targetReachedLabel = "Target reached"
                )
            )
        }

        composeTestRule.onNodeWithText("Target reached").assertIsDisplayed()
    }

    @Test
    fun shotSavedConfirmationIsDisplayedAfterSaving() {
        composeTestRule.activity.setContent {
            CaptureScreen(uiState = CaptureUiStateMapper.savedConfirmation())
        }

        composeTestRule.onNodeWithText("Shot saved").assertIsDisplayed()
    }
}
