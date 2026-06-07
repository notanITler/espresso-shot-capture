package com.example.espressoshotcapture.capture

enum class CaptureStatus {
    READY,
    RECORDING,
    SAVED
}

data class CaptureUiState(
    val status: CaptureStatus,
    val scaleConnectionLabel: String,
    val shotStatusLabel: String,
    val primaryActionLabel: String,
    val isPrimaryActionEnabled: Boolean,
    val currentWeightLabel: String? = null,
    val flowTimeLabel: String? = null,
    val averageFlowLabel: String? = null
)

object CaptureUiStateMapper {
    fun initialDisconnectedReady(): CaptureUiState =
        ready(scaleConnectionLabel = "Scale: Not connected")

    fun ready(scaleConnectionLabel: String): CaptureUiState = CaptureUiState(
        status = CaptureStatus.READY,
        scaleConnectionLabel = scaleConnectionLabel,
        shotStatusLabel = "Ready",
        primaryActionLabel = "Start capture",
        isPrimaryActionEnabled = true
    )

    fun recording(scaleConnectionLabel: String = "Scale: Not connected"): CaptureUiState = CaptureUiState(
        status = CaptureStatus.RECORDING,
        scaleConnectionLabel = scaleConnectionLabel,
        shotStatusLabel = "Recording",
        primaryActionLabel = "Stop & save",
        isPrimaryActionEnabled = true,
        currentWeightLabel = "Weight: 0.0 g",
        flowTimeLabel = "Flow time: 0 s",
        averageFlowLabel = "Average flow: 0.0 g/s"
    )

    fun savedConfirmation(scaleConnectionLabel: String = "Scale: Not connected"): CaptureUiState = CaptureUiState(
        status = CaptureStatus.SAVED,
        scaleConnectionLabel = scaleConnectionLabel,
        shotStatusLabel = "Shot saved",
        primaryActionLabel = "Start capture",
        isPrimaryActionEnabled = false
    )
}
