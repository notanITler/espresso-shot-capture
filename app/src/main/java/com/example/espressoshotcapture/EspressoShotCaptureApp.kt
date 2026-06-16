package com.example.espressoshotcapture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.espressoshotcapture.ble.BleScaleScanRoute
import com.example.espressoshotcapture.capture.CaptureRoute
import com.example.espressoshotcapture.capture.CaptureScreen

@Composable
fun EspressoShotCaptureApp(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101214))
            .verticalScroll(rememberScrollState())
    ) {
        CaptureRoute()
        com.example.espressoshotcapture.history.ShotHistoryRoute()
        BleScaleScanRoute()
    }
}

@Preview
@Composable
private fun EspressoShotCaptureAppPreview() {
    Column {
        CaptureScreen()
    }
}
