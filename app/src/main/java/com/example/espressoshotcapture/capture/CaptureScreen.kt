package com.example.espressoshotcapture.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@Composable
fun CaptureScreen(
    uiState: CaptureUiState = CaptureUiStateMapper.initialDisconnectedReady(),
    onPrimaryAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
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
        recordingValuesText(uiState)?.let { valuesText ->
            BasicText(text = valuesText)
        }
    }
}

private fun recordingValuesText(uiState: CaptureUiState): String? =
    listOfNotNull(
        uiState.currentWeightLabel,
        uiState.flowTimeLabel,
        uiState.averageFlowLabel
    ).takeIf { labels -> labels.isNotEmpty() }
        ?.joinToString(separator = "  |  ")

@Preview
@Composable
private fun CaptureScreenPreview() {
    CaptureScreen()
}
