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
        currentWeightLabel = "Weight: 36.8 g",
        flowTimeLabel = "Flow time: 28 s",
        averageFlowLabel = "Average flow: 1.3 g/s"
    )

    fun savedConfirmation(scaleConnectionLabel: String = "Scale: Not connected"): CaptureUiState = CaptureUiState(
        status = CaptureStatus.SAVED,
        scaleConnectionLabel = scaleConnectionLabel,
        shotStatusLabel = "Shot saved",
        primaryActionLabel = "Start capture",
        isPrimaryActionEnabled = false
    )
}
