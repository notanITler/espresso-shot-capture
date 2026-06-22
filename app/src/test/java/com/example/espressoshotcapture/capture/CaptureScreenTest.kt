package com.example.espressoshotcapture.capture

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
        setScrollableCaptureContent()

        composeTestRule.onAllNodesWithText("Espresso Shot Capture").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Scale / source").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Scale: Not connected").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Fake scale simulation").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Capture source: Fake scale/demo").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Dose / target / ratio").assertCountEquals(0)
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.FAKE_SOURCE)
            .assertTextContains("Demo mode")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.DECENT_SOURCE)
            .assertTextContains("Decent Scale")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.DOSE_INPUT)
            .assertIsDisplayed()
            .assertTextContains("18.0")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.TARGET_YIELD_INPUT)
            .assertIsDisplayed()
            .assertTextContains("36.0")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.RATIO_DISPLAY).assertTextContains("Ratio: 1:2")
        composeTestRule.onNodeWithText("Progress: 0.0 / 36.0 g")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Target not reached")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.STATUS)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Ready")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.TARE_ACTION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Tare")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.PRIMARY_ACTION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Start capture")
    }

    @Test
    fun defaultUiStateHasPlaceholderLabels() {
        val uiState = CaptureUiStateMapper.initialDisconnectedReady()

        setScrollableCaptureContent(uiState = uiState)

        composeTestRule.onAllNodesWithText("Scale: Not connected").assertCountEquals(0)
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.DOSE_INPUT).assertTextContains("18.0")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.TARGET_YIELD_INPUT).assertTextContains("36.0")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.RATIO_DISPLAY).assertTextContains("Ratio: 1:2")
        composeTestRule.onNodeWithText("Progress: 0.0 / 36.0 g")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Target not reached")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.STATUS)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Ready")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.PRIMARY_ACTION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Start capture")
    }

    @Test
    fun demoScaleLabelIsNotShownAsRedundantCopy() {
        setScrollableCaptureContent(
            uiState = CaptureUiStateMapper.initialDisconnectedReady(
                scaleModeLabel = "Fake scale simulation"
            )
        )

        composeTestRule.onAllNodesWithText("Scale: Not connected").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Fake scale simulation").assertCountEquals(0)
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.FAKE_SOURCE)
            .assertTextContains("Demo mode")
    }

    @Test
    fun sourceSelectionInvokesCallbacks() {
        var fakeSelected = false
        var realSelected = false

        composeTestRule.activity.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CaptureScreen(
                    onFakeScaleSelected = { fakeSelected = true },
                    onDecentScaleSelected = { realSelected = true }
                )
            }
        }

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.FAKE_SOURCE)
            .performClick()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.DECENT_SOURCE)
            .performClick()

        composeTestRule.runOnIdle {
            assertTrue(fakeSelected)
            assertTrue(realSelected)
        }
    }

    @Test
    fun editingDoseUpdatesRatioDisplay() {
        composeTestRule.activity.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                var targetState by remember {
                    mutableStateOf(MvpShotTarget.defaultState())
                }
                CaptureScreen(
                    targetState = targetState,
                    onDoseChanged = { input ->
                        targetState = CaptureTargetState(
                            doseGrams = input.toDoubleOrNull() ?: Double.NaN,
                            targetYieldGrams = targetState.targetYieldGrams
                        )
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.DOSE_INPUT)
            .performTextClearance()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.DOSE_INPUT)
            .performTextInput("20.0")

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.RATIO_DISPLAY)
            .assertTextContains("Ratio: 1:1.8")
    }

    @Test
    fun editingTargetYieldUpdatesRatioDisplay() {
        composeTestRule.activity.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                var targetState by remember {
                    mutableStateOf(MvpShotTarget.defaultState())
                }
                CaptureScreen(
                    targetState = targetState,
                    onTargetYieldChanged = { input ->
                        targetState = CaptureTargetState(
                            doseGrams = targetState.doseGrams,
                            targetYieldGrams = input.toDoubleOrNull() ?: Double.NaN
                        )
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.TARGET_YIELD_INPUT)
            .performTextClearance()
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.TARGET_YIELD_INPUT)
            .performTextInput("45.0")

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.RATIO_DISPLAY)
            .assertTextContains("Ratio: 1:2.5")
    }

    @Test
    fun invalidTargetShowsValidationMessage() {
        setScrollableCaptureContent(
            targetState = CaptureTargetState(
                doseGrams = 0.0,
                targetYieldGrams = 36.0
            )
        )

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.VALIDATION_MESSAGE)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Enter a positive dose and target yield.")
    }

    @Test
    fun primaryActionInvokesCallback() {
        var clicked = false

        setScrollableCaptureContent(
            uiState = CaptureUiStateMapper.initialDisconnectedReady(),
            onPrimaryAction = { clicked = true }
        )

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.PRIMARY_ACTION)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        composeTestRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun tareActionInvokesCallbackWhenEnabledForRealScale() {
        var tared = false

        setScrollableCaptureContent(
            uiState = CaptureUiStateMapper.ready(
                scaleConnectionLabel = "Scale: Connected",
                selectedScaleSource = CaptureScaleSource.DECENT,
                captureSourceStatusLabel = "Capture source: Decent Scale/real ready"
            ),
            isTareEnabled = true,
            onTare = { tared = true }
        )

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.TARE_ACTION)
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        composeTestRule.runOnIdle {
            assertTrue(tared)
        }
    }

    @Test
    fun realScaleUnavailableShowsStableActionStatus() {
        setScrollableCaptureContent(
            uiState = CaptureUiStateMapper.ready(
                scaleConnectionLabel = "Scale: Not connected",
                selectedScaleSource = CaptureScaleSource.DECENT,
                captureSourceStatusLabel = "Capture source: Decent Scale/real unavailable",
                captureSourceMessage = "Connect Decent Scale in BLE debug first.",
                isPrimaryActionEnabled = false
            )
        )

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.PRIMARY_ACTION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Start capture")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.ACTION_STATUS)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Connect Decent Scale in BLE debug first.")
    }

    @Test
    fun stopAndSaveIsShownInRecording() {
        setScrollableCaptureContent(uiState = CaptureUiStateMapper.recording())

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.STATUS)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Recording")
        composeTestRule.onNodeWithTag(CaptureScreenTestTags.PRIMARY_ACTION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Stop & save")
    }

    @Test
    fun recordingValuesAreDisplayedWhileRecording() {
        composeTestRule.activity.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CaptureScreen(uiState = CaptureUiStateMapper.recording())
            }
        }

        composeTestRule
            .onNodeWithTag(CaptureScreenTestTags.RECORDING_WEIGHT)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Weight: 0.0 g")
        composeTestRule
            .onNodeWithTag(CaptureScreenTestTags.RECORDING_PROGRESS)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Progress: 0.0 / 36.0 g")
        composeTestRule
            .onNodeWithTag(CaptureScreenTestTags.TARGET_REACHED)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Target not reached")
        composeTestRule
            .onNodeWithTag(CaptureScreenTestTags.RECORDING_CAPTURE_ELAPSED)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Capture elapsed: 0 s")
        composeTestRule
            .onNodeWithTag(CaptureScreenTestTags.RECORDING_AVERAGE_FLOW)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Average flow: 0.0 g/s")
    }

    @Test
    fun targetReachedIsDisplayedWhenProvidedByUiState() {
        setScrollableCaptureContent(
            uiState = CaptureUiStateMapper.recording().copy(
                currentWeightLabel = "Weight: 36.0 g",
                progressLabel = "Progress: 36.0 / 36.0 g",
                targetReachedLabel = "Target reached"
            )
        )

        composeTestRule.onNodeWithText("Target reached")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun shotSavedConfirmationIsDisplayedAfterSaving() {
        setScrollableCaptureContent(uiState = CaptureUiStateMapper.savedConfirmation())

        composeTestRule.onNodeWithTag(CaptureScreenTestTags.STATUS)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Shot saved")
    }

    private fun setScrollableCaptureContent(
        uiState: CaptureUiState = CaptureUiStateMapper.initialDisconnectedReady(),
        targetState: CaptureTargetState = MvpShotTarget.defaultState(),
        onPrimaryAction: () -> Unit = {},
        isTareEnabled: Boolean = false,
        onTare: () -> Unit = {}
    ) {
        composeTestRule.activity.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CaptureScreen(
                    uiState = uiState,
                    targetState = targetState,
                    onPrimaryAction = onPrimaryAction,
                    isTareEnabled = isTareEnabled,
                    onTare = onTare
                )
            }
        }
    }
}
