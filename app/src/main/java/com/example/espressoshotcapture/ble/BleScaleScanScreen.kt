package com.example.espressoshotcapture.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication
import com.example.espressoshotcapture.ui.SectionContainer

@Composable
fun BleScaleScanRoute(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as EspressoShotCaptureApplication
    val viewModel: BleScaleScanViewModel = viewModel(
        factory = BleScaleScanViewModel.factory(
            scanner = application.appContainer.bleScaleScanner,
            gattClient = application.appContainer.decentScaleGattClient,
            onExpectedScaleSelected = application.appContainer::selectDecentScaleCandidate
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val gattState by viewModel.gattState.collectAsState()
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
        gattState = gattState,
        onStartScan = {
            permissionLauncher.launch(AndroidBleScaleScanner.requiredRuntimePermissions())
        },
        onStopScan = viewModel::stopScan,
        onCandidateSelected = viewModel::connect,
        modifier = modifier
    )
}

@Composable
fun BleScaleScanScreen(
    uiState: BleScaleScanState,
    gattState: DecentScaleGattState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onCandidateSelected: (BleScaleScanCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        SectionContainer(title = "BLE scale discovery") {
            Spacer(modifier = Modifier.height(6.dp))
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    uiState.candidates.forEach { candidate ->
                        val candidateModifier = if (candidate.isExpectedScale) {
                            Modifier
                                .fillMaxWidth()
                                .clickable { onCandidateSelected(candidate) }
                        } else {
                            Modifier.fillMaxWidth()
                        }
                        Column(modifier = candidateModifier) {
                            BasicText(text = candidate.displayName)
                            BasicText(text = candidate.displayAddress)
                            BasicText(text = candidate.matchLabel)
                            BasicText(text = candidate.rssiLabel)
                            BasicText(text = candidate.serviceUuidsLabel)
                            if (candidate.isExpectedScale) {
                                BasicText(text = "Tap to connect")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        SectionContainer(title = "Decent Scale GATT debug") {
            Spacer(modifier = Modifier.height(6.dp))
            BasicText(text = gattState.connectionLabel)
            BasicText(text = gattState.notifyCharacteristicLabel)
            BasicText(text = gattState.writeCharacteristicLabel)
            BasicText(text = gattState.latestRawPacketLabel)
            BasicText(text = gattState.latestWeightLabel)
            BasicText(text = gattState.latestTimestampLabel)
            gattState.latestParserError?.let { parserError ->
                BasicText(text = "Parser: $parserError")
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
        gattState = DecentScaleGattState(),
        onStartScan = {},
        onStopScan = {},
        onCandidateSelected = {}
    )
}
