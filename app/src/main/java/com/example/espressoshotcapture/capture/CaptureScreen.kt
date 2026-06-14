package com.example.espressoshotcapture.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.espressoshotcapture.ui.SectionContainer

@Composable
fun CaptureScreen(
    uiState: CaptureUiState = CaptureUiStateMapper.initialDisconnectedReady(),
    targetState: CaptureTargetState = MvpShotTarget.defaultState(),
    onDoseChanged: (String) -> Unit = {},
    onTargetYieldChanged: (String) -> Unit = {},
    onFakeScaleSelected: () -> Unit = {},
    onDecentScaleSelected: () -> Unit = {},
    onPrimaryAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPrimaryActionEnabled = uiState.isPrimaryActionEnabled &&
        (uiState.status != CaptureStatus.READY || targetState.isValid)

    SectionContainer(
        title = "Capture",
        modifier = modifier
    ) {
        BasicText(
            text = "Espresso Shot Capture",
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(text = uiState.scaleConnectionLabel)
        uiState.scaleModeLabel?.let { scaleModeLabel ->
            BasicText(text = scaleModeLabel)
        }
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = "Capture source",
            style = TextStyle(fontWeight = FontWeight.SemiBold)
        )
        BasicText(
            text = uiState.fakeScaleSourceLabel(),
            modifier = Modifier
                .testTag(CaptureScreenTestTags.FAKE_SOURCE)
                .clickable(onClick = onFakeScaleSelected)
        )
        BasicText(
            text = uiState.decentScaleSourceLabel(),
            modifier = Modifier
                .testTag(CaptureScreenTestTags.DECENT_SOURCE)
                .clickable(onClick = onDecentScaleSelected)
        )
        BasicText(
            text = uiState.captureSourceStatusLabel,
            modifier = Modifier.testTag(CaptureScreenTestTags.SOURCE_STATUS)
        )
        uiState.captureSourceMessage?.let { sourceMessage ->
            BasicText(
                text = sourceMessage,
                modifier = Modifier.testTag(CaptureScreenTestTags.SOURCE_MESSAGE)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = "Target",
            style = TextStyle(fontWeight = FontWeight.SemiBold)
        )
        BasicText(text = "Dose in grams")
        BasicTextField(
            value = targetState.doseInputValue,
            onValueChange = onDoseChanged,
            singleLine = true,
            modifier = Modifier.testTag(CaptureScreenTestTags.DOSE_INPUT)
        )
        BasicText(text = "Target yield in grams")
        BasicTextField(
            value = targetState.targetYieldInputValue,
            onValueChange = onTargetYieldChanged,
            singleLine = true,
            modifier = Modifier.testTag(CaptureScreenTestTags.TARGET_YIELD_INPUT)
        )
        BasicText(
            text = targetState.ratioLabel,
            modifier = Modifier.testTag(CaptureScreenTestTags.RATIO_DISPLAY)
        )
        targetState.validationMessage?.let { validationMessage ->
            BasicText(
                text = validationMessage,
                modifier = Modifier.testTag(CaptureScreenTestTags.VALIDATION_MESSAGE)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = "Live shot",
            style = TextStyle(fontWeight = FontWeight.SemiBold)
        )
        BasicText(text = uiState.progressLabel)
        BasicText(text = uiState.targetReachedLabel)
        BasicText(
            text = uiState.shotStatusLabel,
            modifier = Modifier.testTag(CaptureScreenTestTags.STATUS)
        )
        BasicText(
            text = uiState.primaryActionLabel,
            modifier = if (isPrimaryActionEnabled) {
                Modifier
                    .testTag(CaptureScreenTestTags.PRIMARY_ACTION)
                    .clickable(onClick = onPrimaryAction)
            } else {
                Modifier.testTag(CaptureScreenTestTags.PRIMARY_ACTION)
            }
        )
        uiState.currentWeightLabel?.let { currentWeightLabel ->
            BasicText(
                text = currentWeightLabel,
                modifier = Modifier.testTag(CaptureScreenTestTags.RECORDING_WEIGHT)
            )
        }
        uiState.captureElapsedLabel?.let { captureElapsedLabel ->
            BasicText(
                text = captureElapsedLabel,
                modifier = Modifier.testTag(CaptureScreenTestTags.RECORDING_CAPTURE_ELAPSED)
            )
        }
        uiState.averageFlowLabel?.let { averageFlowLabel ->
            BasicText(
                text = averageFlowLabel,
                modifier = Modifier.testTag(CaptureScreenTestTags.RECORDING_AVERAGE_FLOW)
            )
        }
    }
}

object CaptureScreenTestTags {
    const val FAKE_SOURCE = "fake-scale-source"
    const val DECENT_SOURCE = "decent-scale-source"
    const val SOURCE_STATUS = "capture-source-status"
    const val SOURCE_MESSAGE = "capture-source-message"
    const val DOSE_INPUT = "dose-input"
    const val TARGET_YIELD_INPUT = "target-yield-input"
    const val RATIO_DISPLAY = "ratio-display"
    const val VALIDATION_MESSAGE = "target-validation-message"
    const val PRIMARY_ACTION = "primary-capture-action"
    const val STATUS = "capture-status"
    const val RECORDING_WEIGHT = "recording-weight"
    const val RECORDING_CAPTURE_ELAPSED = "recording-capture-elapsed"
    const val RECORDING_AVERAGE_FLOW = "recording-average-flow"
}

private fun CaptureUiState.fakeScaleSourceLabel(): String =
    if (selectedScaleSource == CaptureScaleSource.FAKE) {
        "Selected: Fake scale/demo"
    } else {
        "Fake scale/demo"
    }

private fun CaptureUiState.decentScaleSourceLabel(): String =
    if (selectedScaleSource == CaptureScaleSource.DECENT) {
        "Selected: Decent Scale/real"
    } else {
        "Decent Scale/real"
    }

@Preview
@Composable
private fun CaptureScreenPreview() {
    CaptureScreen()
}
