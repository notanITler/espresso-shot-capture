package com.example.espressoshotcapture.capture

enum class CaptureStatus {
    READY,
    RECORDING,
    SAVED
}

data class CaptureUiState(
    val status: CaptureStatus,
    val scaleConnectionLabel: String,
    val scaleModeLabel: String? = null,
    val shotStatusLabel: String,
    val primaryActionLabel: String,
    val isPrimaryActionEnabled: Boolean,
    val doseLabel: String = MvpShotTarget.DOSE_LABEL,
    val targetYieldLabel: String = MvpShotTarget.TARGET_YIELD_LABEL,
    val ratioLabel: String = MvpShotTarget.RATIO_LABEL,
    val progressLabel: String = MvpShotTarget.EMPTY_PROGRESS_LABEL,
    val targetReachedLabel: String = MvpShotTarget.TARGET_NOT_REACHED_LABEL,
    val currentWeightLabel: String? = null,
    val flowTimeLabel: String? = null,
    val averageFlowLabel: String? = null
)

object CaptureUiStateMapper {
    fun initialDisconnectedReady(scaleModeLabel: String? = null): CaptureUiState =
        ready(
            scaleConnectionLabel = "Scale: Not connected",
            scaleModeLabel = scaleModeLabel
        )

    fun ready(
        scaleConnectionLabel: String,
        scaleModeLabel: String? = null
    ): CaptureUiState = CaptureUiState(
        status = CaptureStatus.READY,
        scaleConnectionLabel = scaleConnectionLabel,
        scaleModeLabel = scaleModeLabel,
        shotStatusLabel = "Ready",
        primaryActionLabel = "Start capture",
        isPrimaryActionEnabled = true
    )

    fun recording(
        scaleConnectionLabel: String = "Scale: Not connected",
        scaleModeLabel: String? = null
    ): CaptureUiState = CaptureUiState(
        status = CaptureStatus.RECORDING,
        scaleConnectionLabel = scaleConnectionLabel,
        scaleModeLabel = scaleModeLabel,
        shotStatusLabel = "Recording",
        primaryActionLabel = "Stop & save",
        isPrimaryActionEnabled = true,
        currentWeightLabel = "Weight: 0.0 g",
        flowTimeLabel = "Flow time: 0 s",
        averageFlowLabel = "Average flow: 0.0 g/s"
    )

    fun savedConfirmation(
        scaleConnectionLabel: String = "Scale: Not connected",
        scaleModeLabel: String? = null
    ): CaptureUiState = CaptureUiState(
        status = CaptureStatus.SAVED,
        scaleConnectionLabel = scaleConnectionLabel,
        scaleModeLabel = scaleModeLabel,
        shotStatusLabel = "Shot saved",
        primaryActionLabel = "Start capture",
        isPrimaryActionEnabled = false
    )
}
