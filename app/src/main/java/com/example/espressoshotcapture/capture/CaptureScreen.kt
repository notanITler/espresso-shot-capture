package com.example.espressoshotcapture.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@Composable
fun CaptureScreen(
    uiState: CaptureUiState = CaptureUiStateMapper.initialDisconnectedReady(),
    targetState: CaptureTargetState = MvpShotTarget.defaultState(),
    onDoseChanged: (String) -> Unit = {},
    onTargetYieldChanged: (String) -> Unit = {},
    onFakeScaleSelected: () -> Unit = {},
    onDecentScaleSelected: () -> Unit = {},
    onPrimaryAction: () -> Unit = {},
    modifier: Modifier = Modifier,
    isTareEnabled: Boolean = false,
    tareStatusLabel: String? = null,
    onTare: () -> Unit = {}
) {
    val isPrimaryActionEnabled = uiState.isPrimaryActionEnabled &&
        (uiState.status != CaptureStatus.READY || targetState.isValid)
    val canTareFromCapture = isTareEnabled &&
        uiState.selectedScaleSource == CaptureScaleSource.DECENT
    val actionStatusLabel = targetState.validationMessage
        ?: uiState.captureSourceMessage
        ?: tareStatusLabel.takeIf { uiState.selectedScaleSource == CaptureScaleSource.DECENT }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(
                color = Color(0xFF171A1E),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF30363D),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        BasicText(
            text = "Espresso Shot Capture",
            style = TextStyle(
                color = Color(0xFFF6F7F9),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        BasicText(
            text = "Scale / source",
            style = captureSectionLabelStyle()
        )
        Spacer(modifier = Modifier.height(2.dp))
        BasicText(
            text = uiState.scaleConnectionLabel,
            style = captureBodyStyle()
        )
        uiState.scaleModeLabel?.let { scaleModeLabel ->
            BasicText(
                text = scaleModeLabel,
                style = captureMutedStyle()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourcePill(
                text = uiState.fakeScaleSourceLabel(),
                selected = uiState.selectedScaleSource == CaptureScaleSource.FAKE,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CaptureScreenTestTags.FAKE_SOURCE)
                    .clickable(onClick = onFakeScaleSelected)
            )
            SourcePill(
                text = uiState.decentScaleSourceLabel(),
                selected = uiState.selectedScaleSource == CaptureScaleSource.DECENT,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CaptureScreenTestTags.DECENT_SOURCE)
                    .clickable(onClick = onDecentScaleSelected)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        BasicText(
            text = uiState.captureSourceStatusLabel,
            modifier = Modifier.testTag(CaptureScreenTestTags.SOURCE_STATUS),
            style = captureMutedStyle()
        )
        uiState.captureSourceMessage?.let { sourceMessage ->
            BasicText(
                text = sourceMessage,
                modifier = Modifier.testTag(CaptureScreenTestTags.SOURCE_MESSAGE),
                style = captureWarningStyle()
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        BasicText(
            text = "Dose / target / ratio",
            style = captureSectionLabelStyle()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TargetInput(
                label = "Dose g",
                value = targetState.doseInputValue,
                onValueChange = onDoseChanged,
                testTag = CaptureScreenTestTags.DOSE_INPUT,
                modifier = Modifier
                    .weight(1f)
            )
            TargetInput(
                label = "Yield g",
                value = targetState.targetYieldInputValue,
                onValueChange = onTargetYieldChanged,
                testTag = CaptureScreenTestTags.TARGET_YIELD_INPUT,
                modifier = Modifier
                    .weight(1f)
            )
            RatioMetric(
                text = targetState.ratioLabel,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CaptureScreenTestTags.RATIO_DISPLAY)
            )
        }
        targetState.validationMessage?.let { validationMessage ->
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(
                text = validationMessage,
                modifier = Modifier.testTag(CaptureScreenTestTags.VALIDATION_MESSAGE),
                style = captureWarningStyle()
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        BasicText(
            text = "Live shot values",
            style = captureSectionLabelStyle()
        )
        Spacer(modifier = Modifier.height(4.dp))
        LargeMetric(
            text = uiState.currentWeightLabel ?: "Weight: 0.0 g",
            modifier = Modifier.testTag(CaptureScreenTestTags.RECORDING_WEIGHT)
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = uiState.progressLabel,
            modifier = Modifier.testTag(CaptureScreenTestTags.RECORDING_PROGRESS),
            style = captureBodyStyle()
        )
        BasicText(
            text = uiState.targetReachedLabel,
            modifier = Modifier.testTag(CaptureScreenTestTags.TARGET_REACHED),
            style = captureMutedStyle()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SupportingMetric(
                text = uiState.captureElapsedLabel ?: "Capture elapsed: 0 s",
                modifier = Modifier
                    .weight(1f)
                    .testTag(CaptureScreenTestTags.RECORDING_CAPTURE_ELAPSED)
            )
            SupportingMetric(
                text = uiState.averageFlowLabel ?: "Average flow: 0.0 g/s",
                modifier = Modifier
                    .weight(1f)
                    .testTag(CaptureScreenTestTags.RECORDING_AVERAGE_FLOW)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        BasicText(
            text = "Shot state",
            style = captureSectionLabelStyle()
        )
        Spacer(modifier = Modifier.height(2.dp))
        BasicText(
            text = uiState.shotStatusLabel,
            modifier = Modifier.testTag(CaptureScreenTestTags.STATUS),
            style = TextStyle(
                color = Color(0xFFF6F7F9),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        StableActionArea(
            primaryActionLabel = uiState.primaryActionLabel,
            isPrimaryActionEnabled = isPrimaryActionEnabled,
            isTareEnabled = canTareFromCapture,
            statusLabel = actionStatusLabel,
            onPrimaryAction = onPrimaryAction,
            onTare = onTare
        )
    }
}

@Composable
private fun SourcePill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .background(
                color = if (selected) Color(0xFF263D37) else Color(0xFF20242A),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF68D391) else Color(0xFF3A414A),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = if (selected) Color(0xFFD8FFE8) else Color(0xFFD0D6DD),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun TargetInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BasicText(
            text = label,
            style = captureMutedStyle()
        )
        Spacer(modifier = Modifier.height(3.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFFF6F7F9),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .testTag(testTag)
                .fillMaxWidth()
                .background(
                    color = Color(0xFF0F1114),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF3A414A),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RatioMetric(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        BasicText(
            text = "Ratio",
            style = captureMutedStyle()
        )
        Spacer(modifier = Modifier.height(3.dp))
        BasicText(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF20242A),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF3A414A),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            style = TextStyle(
                color = Color(0xFFE6EBF0),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun LargeMetric(
    text: String,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = Color(0xFFFFFFFF),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun SupportingMetric(
    text: String,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = text,
        modifier = modifier
            .background(
                color = Color(0xFF20242A),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(8.dp),
        style = TextStyle(
            color = Color(0xFFE6EBF0),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    )
}

@Composable
private fun ActionPill(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = text,
        modifier = modifier
            .background(
                color = if (enabled) Color(0xFFF2C94C) else Color(0xFF24282E),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = if (enabled) Color(0xFFF7D66F) else Color(0xFF3A414A),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        style = TextStyle(
            color = if (enabled) Color(0xFF181A1D) else Color(0xFF6F7782),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun StableActionArea(
    primaryActionLabel: String,
    isPrimaryActionEnabled: Boolean,
    isTareEnabled: Boolean,
    statusLabel: String?,
    onPrimaryAction: () -> Unit,
    onTare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionPill(
            text = "Tare",
            enabled = isTareEnabled,
            modifier = if (isTareEnabled) {
                Modifier
                    .weight(1f)
                    .testTag(CaptureScreenTestTags.TARE_ACTION)
                    .clickable(onClick = onTare)
            } else {
                Modifier
                    .weight(1f)
                    .testTag(CaptureScreenTestTags.TARE_ACTION)
            }
        )
        ActionPill(
            text = primaryActionLabel,
            enabled = isPrimaryActionEnabled,
            modifier = if (isPrimaryActionEnabled) {
                Modifier
                    .weight(2f)
                    .testTag(CaptureScreenTestTags.PRIMARY_ACTION)
                    .clickable(onClick = onPrimaryAction)
            } else {
                Modifier
                    .weight(2f)
                    .testTag(CaptureScreenTestTags.PRIMARY_ACTION)
            }
        )
    }
    BasicText(
        text = statusLabel ?: " ",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag(CaptureScreenTestTags.ACTION_STATUS),
        style = captureMutedStyle()
    )
}

private fun captureSectionLabelStyle(): TextStyle =
    TextStyle(
        color = Color(0xFF97A2AD),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )

private fun captureBodyStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFE6EBF0),
        fontSize = 14.sp
    )

private fun captureMutedStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFAAB2BC),
        fontSize = 13.sp
    )

private fun captureWarningStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFFFC078),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )

object CaptureScreenTestTags {
    const val FAKE_SOURCE = "fake-scale-source"
    const val DECENT_SOURCE = "decent-scale-source"
    const val SOURCE_STATUS = "capture-source-status"
    const val SOURCE_MESSAGE = "capture-source-message"
    const val DOSE_INPUT = "dose-input"
    const val TARGET_YIELD_INPUT = "target-yield-input"
    const val RATIO_DISPLAY = "ratio-display"
    const val VALIDATION_MESSAGE = "target-validation-message"
    const val TARE_ACTION = "tare-action"
    const val TARE_STATUS = "tare-status"
    const val ACTION_STATUS = "capture-action-status"
    const val PRIMARY_ACTION = "primary-capture-action"
    const val STATUS = "capture-status"
    const val RECORDING_WEIGHT = "recording-weight"
    const val RECORDING_PROGRESS = "recording-progress"
    const val RECORDING_CAPTURE_ELAPSED = "recording-capture-elapsed"
    const val RECORDING_AVERAGE_FLOW = "recording-average-flow"
    const val TARGET_REACHED = "target-reached"
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
