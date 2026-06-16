package com.example.espressoshotcapture.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication

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
        onTare = viewModel::sendTare,
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
    onTare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        DebugBleContainer(title = "Debug / BLE") {
            BasicText(text = uiState.statusLabel)
            BasicText(text = gattState.connectionLabel)
            Spacer(modifier = Modifier.height(6.dp))
            BasicText(
                text = if (isExpanded) "Hide Debug / BLE" else "Show Debug / BLE",
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                DebugSectionTitle(text = "BLE scale discovery")
                Spacer(modifier = Modifier.height(6.dp))
                BleDiscoveryContent(
                    uiState = uiState,
                    onStartScan = onStartScan,
                    onStopScan = onStopScan,
                    onCandidateSelected = onCandidateSelected
                )
                Spacer(modifier = Modifier.height(12.dp))
                DebugSectionTitle(text = "Decent Scale GATT debug")
                Spacer(modifier = Modifier.height(6.dp))
                DecentScaleGattDebugContent(
                    gattState = gattState,
                    onTare = onTare
                )
            }
        }
    }
}

@Composable
private fun DebugBleContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(
                color = Color(0xFFF8F8F8),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        BasicText(
            text = title,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        )
        content()
    }
}

@Composable
private fun BleDiscoveryContent(
    uiState: BleScaleScanState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onCandidateSelected: (BleScaleScanCandidate) -> Unit
) {
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

@Composable
private fun DecentScaleGattDebugContent(
    gattState: DecentScaleGattState,
    onTare: () -> Unit
) {
    BasicText(text = gattState.notifyCharacteristicLabel)
    BasicText(text = gattState.writeCharacteristicLabel)
    BasicText(text = gattState.tareStatusLabel)
    if (gattState.canSendTare) {
        BasicText(
            text = "Tare",
            modifier = Modifier.clickable(onClick = onTare)
        )
    }
    BasicText(text = gattState.latestRawPacketLabel)
    BasicText(text = gattState.latestWeightLabel)
    BasicText(text = gattState.latestTimestampLabel)
    gattState.latestParserError?.let { parserError ->
        BasicText(text = "Parser: $parserError")
    }
}

@Composable
private fun DebugSectionTitle(text: String) {
    BasicText(
        text = text,
        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    )
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
        onCandidateSelected = {},
        onTare = {}
    )
}
