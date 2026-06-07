package com.example.espressoshotcapture.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
        BasicText(text = "Espresso Shot Capture")
        BasicText(text = uiState.scaleConnectionLabel)
        BasicText(text = uiState.shotStatusLabel)
        uiState.currentWeightLabel?.let { label ->
            BasicText(text = label)
        }
        uiState.flowTimeLabel?.let { label ->
            BasicText(text = label)
        }
        uiState.averageFlowLabel?.let { label ->
            BasicText(text = label)
        }
        BasicText(
            text = uiState.primaryActionLabel,
            modifier = if (uiState.isPrimaryActionEnabled) {
                Modifier.clickable(onClick = onPrimaryAction)
            } else {
                Modifier
            }
        )
        BasicText(text = "Shot History")
    }
}

@Preview
@Composable
private fun CaptureScreenPreview() {
    CaptureScreen()
}
