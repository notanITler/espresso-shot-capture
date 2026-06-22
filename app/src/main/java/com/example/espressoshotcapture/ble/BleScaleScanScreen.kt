package com.example.espressoshotcapture.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication

@Composable
fun ScaleConnectionRoute(
    modifier: Modifier = Modifier
) {
    val viewModel = rememberBleScaleScanViewModel()
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

    ScaleConnectionHeader(
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
fun BleScaleScanRoute(
    modifier: Modifier = Modifier
) {
    val viewModel = rememberBleScaleScanViewModel()
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
private fun rememberBleScaleScanViewModel(): BleScaleScanViewModel {
    val context = LocalContext.current
    val application = context.applicationContext as EspressoShotCaptureApplication
    return viewModel(
        factory = BleScaleScanViewModel.factory(
            scanner = application.appContainer.bleScaleScanner,
            gattClient = application.appContainer.decentScaleGattClient,
            onExpectedScaleSelected = application.appContainer::selectDecentScaleCandidate
        )
    )
}

@Composable
fun ScaleConnectionHeader(
    uiState: BleScaleScanState,
    gattState: DecentScaleGattState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onCandidateSelected: (BleScaleScanCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPanelVisible by remember { mutableStateOf(false) }
    val state = bluetoothHeaderState(uiState, gattState)

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
                color = state.borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            BasicText(
                text = "David's Coffee Companion",
                modifier = Modifier
                    .weight(1f)
                    .testTag(ScaleConnectionTestTags.HEADER_TITLE),
                style = TextStyle(
                    color = Color(0xFFF6F7F9),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                text = state.buttonLabel,
                modifier = Modifier
                    .background(
                        color = state.buttonColor,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = state.borderColor,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { isPanelVisible = !isPanelVisible }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag(ScaleConnectionTestTags.BLUETOOTH_BUTTON),
                style = TextStyle(
                    color = state.buttonTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        if (isPanelVisible) {
            Spacer(modifier = Modifier.height(12.dp))
            ScaleConnectionPanel(
                uiState = uiState,
                gattState = gattState,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onCandidateSelected = onCandidateSelected,
                modifier = Modifier.testTag(ScaleConnectionTestTags.PANEL)
            )
        }
    }
}

@Composable
private fun ScaleConnectionPanel(
    uiState: BleScaleScanState,
    gattState: DecentScaleGattState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onCandidateSelected: (BleScaleScanCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    val matchingCandidate = uiState.candidates.firstOrNull { candidate ->
        candidate.isExpectedScale
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF20242A),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        BasicText(
            text = "Scale Connection",
            style = TextStyle(
                color = Color(0xFFF6F7F9),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = "Source: Decent Scale",
            style = scaleConnectionBodyStyle()
        )
        BasicText(
            text = userScaleStatusLabel(uiState, gattState),
            style = scaleConnectionBodyStyle()
        )
        BasicText(
            text = gattState.latestWeightLabel,
            style = scaleConnectionBodyStyle()
        )
        userScaleMessage(uiState, gattState)?.let { message ->
            BasicText(
                text = message,
                style = scaleConnectionMutedStyle()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = if (uiState.status == BleScaleScanStatus.SCANNING) {
                "Stop scan"
            } else {
                "Start scan"
            },
            modifier = Modifier
                .clickable(
                    onClick = if (uiState.status == BleScaleScanStatus.SCANNING) {
                        onStopScan
                    } else {
                        onStartScan
                    }
                )
                .testTag(ScaleConnectionTestTags.SCAN_ACTION),
            style = scaleConnectionActionStyle()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (matchingCandidate == null) {
            BasicText(
                text = "No Decent Scale found yet",
                style = scaleConnectionMutedStyle()
            )
        } else {
            val candidateActionLabel = decentScaleCandidateActionLabel(gattState)
            val candidateModifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isDecentScaleCandidateConnectEnabled(gattState)) {
                        Modifier.clickable { onCandidateSelected(matchingCandidate) }
                    } else {
                        Modifier
                    }
                )
            Column(
                modifier = candidateModifier
                    .testTag(ScaleConnectionTestTags.CANDIDATE)
                    .background(
                        color = Color(0xFF263D37),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF68D391),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(10.dp)
            ) {
                BasicText(
                    text = "Decent Scale candidate: ${matchingCandidate.displayName}",
                    style = scaleConnectionBodyStyle()
                )
                BasicText(
                    text = matchingCandidate.displayAddress,
                    style = scaleConnectionMutedStyle()
                )
                BasicText(
                    text = candidateActionLabel,
                    style = scaleConnectionActionStyle()
                )
            }
        }
    }
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
            BasicText(text = uiState.statusLabel, style = debugBleBodyStyle())
            BasicText(text = gattState.connectionLabel, style = debugBleBodyStyle())
            Spacer(modifier = Modifier.height(6.dp))
            BasicText(
                text = if (isExpanded) "Hide Debug / BLE" else "Show Debug / BLE",
                modifier = Modifier.clickable { isExpanded = !isExpanded },
                style = debugBleActionStyle()
            )
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                DebugSectionTitle(text = "BLE scale discovery")
                Spacer(modifier = Modifier.height(6.dp))
                BleDiscoveryContent(
                    uiState = uiState,
                    gattState = gattState,
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
            text = title,
            style = TextStyle(
                color = Color(0xFFF6F7F9),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        content()
    }
}

@Composable
private fun BleDiscoveryContent(
    uiState: BleScaleScanState,
    gattState: DecentScaleGattState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onCandidateSelected: (BleScaleScanCandidate) -> Unit
) {
    uiState.errorMessage?.let { errorMessage ->
        BasicText(text = errorMessage, style = debugBleMutedStyle())
    }
    Spacer(modifier = Modifier.height(4.dp))
    BasicText(
        text = "Start BLE scan",
        modifier = Modifier.clickable(onClick = onStartScan),
        style = debugBleActionStyle()
    )
    if (uiState.status == BleScaleScanStatus.SCANNING) {
        BasicText(
            text = "Stop BLE scan",
            modifier = Modifier.clickable(onClick = onStopScan),
            style = debugBleActionStyle()
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    if (uiState.candidates.isEmpty()) {
        BasicText(text = "No BLE devices found yet", style = debugBleMutedStyle())
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
                        .then(
                            if (isDecentScaleCandidateConnectEnabled(gattState)) {
                                Modifier.clickable { onCandidateSelected(candidate) }
                            } else {
                                Modifier
                            }
                        )
                } else {
                    Modifier.fillMaxWidth()
                }
                Column(modifier = candidateModifier) {
                    BasicText(text = candidate.displayName, style = debugBleBodyStyle())
                    BasicText(text = candidate.displayAddress, style = debugBleMutedStyle())
                    BasicText(text = candidate.matchLabel, style = debugBleBodyStyle())
                    BasicText(text = candidate.rssiLabel, style = debugBleMutedStyle())
                    BasicText(text = candidate.serviceUuidsLabel, style = debugBleMutedStyle())
                    if (candidate.isExpectedScale) {
                        BasicText(
                            text = decentScaleCandidateActionLabel(gattState),
                            style = debugBleActionStyle()
                        )
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
    BasicText(text = gattState.notifyCharacteristicLabel, style = debugBleBodyStyle())
    BasicText(text = gattState.writeCharacteristicLabel, style = debugBleBodyStyle())
    BasicText(text = gattState.tareStatusLabel, style = debugBleBodyStyle())
    if (gattState.canSendTare) {
        BasicText(
            text = "Tare",
            modifier = Modifier.clickable(onClick = onTare),
            style = debugBleActionStyle()
        )
    }
    BasicText(text = gattState.latestRawPacketLabel, style = debugBleMutedStyle())
    BasicText(text = gattState.latestWeightLabel, style = debugBleBodyStyle())
    BasicText(text = gattState.latestTimestampLabel, style = debugBleMutedStyle())
    gattState.latestParserError?.let { parserError ->
        BasicText(text = "Parser: $parserError", style = debugBleMutedStyle())
    }
}

@Composable
private fun DebugSectionTitle(text: String) {
    BasicText(
        text = text,
        style = TextStyle(
            color = Color(0xFFF6F7F9),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    )
}

private data class BluetoothHeaderState(
    val buttonLabel: String,
    val buttonColor: Color,
    val buttonTextColor: Color,
    val borderColor: Color
)

private fun bluetoothHeaderState(
    uiState: BleScaleScanState,
    gattState: DecentScaleGattState
): BluetoothHeaderState =
    when {
        uiState.status.isUserFacingError() || gattState.connectionState.isUserFacingError() ->
            BluetoothHeaderState(
                buttonLabel = "Bluetooth: error",
                buttonColor = Color(0xFF4A2525),
                buttonTextColor = Color(0xFFFFD7D7),
                borderColor = Color(0xFFFF8787)
            )
        gattState.connectionState == DecentScaleGattConnectionState.Connecting ->
            BluetoothHeaderState(
                buttonLabel = "Bluetooth: connecting",
                buttonColor = Color(0xFF3D3420),
                buttonTextColor = Color(0xFFFFE8A3),
                borderColor = Color(0xFFF2C94C)
            )
        gattState.connectionState == DecentScaleGattConnectionState.ReceivingReadings ->
            BluetoothHeaderState(
                buttonLabel = bluetoothHeaderButtonLabel(uiState, gattState),
                buttonColor = Color(0xFF263D37),
                buttonTextColor = Color(0xFFD8FFE8),
                borderColor = Color(0xFF68D391)
            )
        gattState.connectionState == DecentScaleGattConnectionState.Connected ->
            BluetoothHeaderState(
                buttonLabel = bluetoothHeaderButtonLabel(uiState, gattState),
                buttonColor = Color(0xFF263D37),
                buttonTextColor = Color(0xFFD8FFE8),
                borderColor = Color(0xFF68D391)
            )
        uiState.status == BleScaleScanStatus.SCANNING ->
            BluetoothHeaderState(
                buttonLabel = bluetoothHeaderButtonLabel(uiState, gattState),
                buttonColor = Color(0xFF3D3420),
                buttonTextColor = Color(0xFFFFE8A3),
                borderColor = Color(0xFFF2C94C)
            )
        else ->
            BluetoothHeaderState(
                buttonLabel = bluetoothHeaderButtonLabel(uiState, gattState),
                buttonColor = Color(0xFF24282E),
                buttonTextColor = Color(0xFFD0D6DD),
                borderColor = Color(0xFF3A414A)
            )
    }

internal fun bluetoothHeaderButtonLabel(
    uiState: BleScaleScanState,
    gattState: DecentScaleGattState
): String =
    when {
        uiState.status.isUserFacingError() || gattState.connectionState.isUserFacingError() ->
            "Bluetooth: error"
        gattState.connectionState == DecentScaleGattConnectionState.Connecting ->
            "Bluetooth: connecting"
        gattState.connectionState == DecentScaleGattConnectionState.ReceivingReadings ->
            "Bluetooth: receiving"
        gattState.connectionState == DecentScaleGattConnectionState.Connected ->
            "Bluetooth: connected"
        uiState.status == BleScaleScanStatus.SCANNING -> "Bluetooth: scanning"
        else -> "Bluetooth: disconnected"
    }

internal fun decentScaleCandidateActionLabel(gattState: DecentScaleGattState): String =
    when (gattState.connectionState) {
        DecentScaleGattConnectionState.Connecting -> "Connecting..."
        DecentScaleGattConnectionState.Connected -> "Connected"
        DecentScaleGattConnectionState.ReceivingReadings -> "Receiving readings"
        else -> "Tap to connect"
    }

internal fun isDecentScaleCandidateConnectEnabled(gattState: DecentScaleGattState): Boolean =
    when (gattState.connectionState) {
        DecentScaleGattConnectionState.Connecting,
        DecentScaleGattConnectionState.Connected,
        DecentScaleGattConnectionState.ReceivingReadings -> false
        else -> true
    }

private fun userScaleStatusLabel(
    uiState: BleScaleScanState,
    gattState: DecentScaleGattState
): String =
    when {
        gattState.connectionState == DecentScaleGattConnectionState.Connecting ->
            "Status: connecting"
        gattState.connectionState == DecentScaleGattConnectionState.ReceivingReadings ->
            "Status: receiving readings"
        gattState.connectionState == DecentScaleGattConnectionState.Connected ->
            "Status: connected"
        uiState.status == BleScaleScanStatus.SCANNING -> "Status: scanning for Decent Scale"
        uiState.status.isUserFacingError() || gattState.connectionState.isUserFacingError() ->
            "Status: connection needs attention"
        else -> "Status: not connected"
    }

private fun userScaleMessage(
    uiState: BleScaleScanState,
    gattState: DecentScaleGattState
): String? =
    uiState.errorMessage
        ?: when (val connectionState = gattState.connectionState) {
            DecentScaleGattConnectionState.Disconnected -> "Start a scan, then tap your Decent Scale."
            DecentScaleGattConnectionState.ServiceDiscoveryFailed -> "Scale service discovery failed."
            DecentScaleGattConnectionState.NotificationSetupFailed -> "Scale notifications could not be enabled."
            is DecentScaleGattConnectionState.Error -> connectionState.message
            else -> null
        }

private fun BleScaleScanStatus.isUserFacingError(): Boolean =
    this == BleScaleScanStatus.ERROR ||
        this == BleScaleScanStatus.PERMISSION_REQUIRED ||
        this == BleScaleScanStatus.BLUETOOTH_UNAVAILABLE

private fun DecentScaleGattConnectionState.isUserFacingError(): Boolean =
    this == DecentScaleGattConnectionState.ServiceDiscoveryFailed ||
        this == DecentScaleGattConnectionState.NotificationSetupFailed ||
        this is DecentScaleGattConnectionState.Error

private fun scaleConnectionBodyStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFE6EBF0),
        fontSize = 14.sp
    )

private fun scaleConnectionMutedStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFAAB2BC),
        fontSize = 13.sp
    )

private fun scaleConnectionActionStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFF2C94C),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )

private fun debugBleBodyStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFE6EBF0),
        fontSize = 13.sp
    )

private fun debugBleMutedStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFAAB2BC),
        fontSize = 12.sp
    )

private fun debugBleActionStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFF2C94C),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )

object ScaleConnectionTestTags {
    const val HEADER_TITLE = "app-header-title"
    const val BLUETOOTH_BUTTON = "bluetooth-header-button"
    const val PANEL = "scale-connection-panel"
    const val SCAN_ACTION = "scale-connection-scan-action"
    const val CANDIDATE = "scale-connection-candidate"
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
