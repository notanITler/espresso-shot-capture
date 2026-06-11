package com.example.espressoshotcapture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.espressoshotcapture.ble.BleScaleScanRoute
import com.example.espressoshotcapture.capture.CaptureRoute
import com.example.espressoshotcapture.capture.CaptureScreen
import com.example.espressoshotcapture.history.ShotHistoryRoute

@Composable
fun EspressoShotCaptureApp(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        CaptureRoute()
        BleScaleScanRoute()
        ShotHistoryRoute(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun EspressoShotCaptureAppPreview() {
    Column {
        CaptureScreen()
    }
}
