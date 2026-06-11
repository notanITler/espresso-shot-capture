package com.example.espressoshotcapture.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication

@Composable
fun BleScaleScanRoute(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as EspressoShotCaptureApplication
    val viewModel: BleScaleScanViewModel = viewModel(
        factory = BleScaleScanViewModel.factory(application.appContainer.bleScaleScanner)
    )
    val uiState by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        if (grantResults.values.all { granted -> granted }) {
            viewModel.startScan()
        } else {
            viewModel.onPermissionsDenied()
        }
    }

    BleScaleScanScreen(
        uiState = uiState,
        onStartScan = {
            permissionLauncher.launch(AndroidBleScaleScanner.requiredRuntimePermissions())
        },
        onStopScan = viewModel::stopScan,
        modifier = modifier
    )
}

@Composable
fun BleScaleScanScreen(
    uiState: BleScaleScanState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = "BLE scale discovery",
            style = TextStyle(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(text = uiState.statusLabel)
        uiState.errorMessage?.let { errorMessage ->
            BasicText(text = errorMessage)
        }
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = "Start BLE scan",
            modifier = Modifier.clickable(onClick = onStartScan)
        )
        if (uiState.status == BleScaleScanStatus.SCANNING) {
            BasicText(
                text = "Stop BLE scan",
                modifier = Modifier.clickable(onClick = onStopScan)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (uiState.candidates.isEmpty()) {
            BasicText(text = "No BLE devices found yet")
        } else {
            uiState.candidates.forEach { candidate ->
                BasicText(text = candidate.displayName)
                BasicText(text = candidate.displayAddress)
                BasicText(text = candidate.matchLabel)
            }
        }
    }
}

@Preview
@Composable
private fun BleScaleScanScreenPreview() {
    BleScaleScanScreen(
        uiState = BleScaleScanState(
            candidates = listOf(
                HalfDecentScaleMatcher.toCandidate(
                    name = "Decent Scale",
                    address = "AA:BB:CC:DD:EE:FF",
                    advertisedServiceUuids = listOf(HalfDecentScaleMatcher.SERVICE_UUID_FULL)
                )
            )
        ),
        onStartScan = {},
        onStopScan = {}
    )
}
