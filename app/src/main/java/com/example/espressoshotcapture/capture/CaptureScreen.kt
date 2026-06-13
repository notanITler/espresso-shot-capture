package com.example.espressoshotcapture.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
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
    onPrimaryAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
            text = "Target",
            style = TextStyle(fontWeight = FontWeight.SemiBold)
        )
        BasicText(text = uiState.doseLabel)
        BasicText(text = uiState.targetYieldLabel)
        BasicText(text = uiState.ratioLabel)
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = "Live shot",
            style = TextStyle(fontWeight = FontWeight.SemiBold)
        )
        BasicText(text = uiState.progressLabel)
        BasicText(text = uiState.targetReachedLabel)
        BasicText(text = uiState.shotStatusLabel)
        BasicText(
            text = uiState.primaryActionLabel,
            modifier = if (uiState.isPrimaryActionEnabled) {
                Modifier.clickable(onClick = onPrimaryAction)
            } else {
                Modifier
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
    const val RECORDING_WEIGHT = "recording-weight"
    const val RECORDING_CAPTURE_ELAPSED = "recording-capture-elapsed"
    const val RECORDING_AVERAGE_FLOW = "recording-average-flow"
}

@Preview
@Composable
private fun CaptureScreenPreview() {
    CaptureScreen()
}
