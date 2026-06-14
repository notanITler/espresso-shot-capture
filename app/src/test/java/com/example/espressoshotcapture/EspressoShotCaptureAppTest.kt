package com.example.espressoshotcapture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
        composeTestRule.onNodeWithText("Espresso Shot Capture").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scale: Connected").assertIsDisplayed()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.PRIMARY_ACTION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Start capture")
    }
}
