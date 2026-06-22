package com.example.espressoshotcapture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.espressoshotcapture.ble.ScaleConnectionTestTags
import com.example.espressoshotcapture.capture.CaptureScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EspressoShotCaptureAppTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appRootShowsCaptureSkeleton() {
        composeTestRule.onNodeWithTag(ScaleConnectionTestTags.HEADER_TITLE)
            .assertIsDisplayed()
            .assertTextContains("David's Coffee Companion")
        composeTestRule.onNodeWithTag(ScaleConnectionTestTags.BLUETOOTH_BUTTON)
            .assertIsDisplayed()
            .assertTextContains("Bluetooth: disconnected")
            .performClick()
        composeTestRule.onNodeWithTag(ScaleConnectionTestTags.PANEL).assertIsDisplayed()
        composeTestRule.onNodeWithText("Scale Connection").assertIsDisplayed()

        composeTestRule.onNodeWithTag(ScaleConnectionTestTags.BLUETOOTH_BUTTON)
            .performClick()
        composeTestRule.onAllNodesWithText("Espresso Shot Capture").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Scale / source").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Scale: Not connected").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Scale: Connected").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Fake scale simulation").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Capture source: Fake scale/demo").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Dose / target / ratio").assertCountEquals(0)
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.FAKE_SOURCE)
            .assertIsDisplayed()
            .assertTextContains("Demo mode")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.DECENT_SOURCE)
            .assertIsDisplayed()
            .assertTextContains("Decent Scale")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.DOSE_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.TARGET_YIELD_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.RATIO_DISPLAY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.TARE_ACTION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Tare")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.PRIMARY_ACTION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Start capture")
        composeTestRule.onNodeWithText("Recent Shot History")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Debug / BLE")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Debug / BLE")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Latest raw packet: none").assertCountEquals(0)
    }
}
