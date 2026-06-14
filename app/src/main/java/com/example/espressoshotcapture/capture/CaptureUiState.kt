package com.example.espressoshotcapture.capture

enum class CaptureStatus {
    READY,
    RECORDING,
    SAVED
}

enum class CaptureScaleSource {
    FAKE,
    DECENT
}

data class CaptureUiState(
    val status: CaptureStatus,
    val scaleConnectionLabel: String,
    val scaleModeLabel: String? = null,
    val selectedScaleSource: CaptureScaleSource = CaptureScaleSource.FAKE,
    val captureSourceStatusLabel: String = "Capture source: Fake scale/demo",
    val captureSourceMessage: String? = null,
    val shotStatusLabel: String,
    val primaryActionLabel: String,
    val isPrimaryActionEnabled: Boolean,
    val doseLabel: String = MvpShotTarget.DOSE_LABEL,
    val targetYieldLabel: String = MvpShotTarget.TARGET_YIELD_LABEL,
    val ratioLabel: String = MvpShotTarget.RATIO_LABEL,
    val progressLabel: String = MvpShotTarget.EMPTY_PROGRESS_LABEL,
    val targetReachedLabel: String = MvpShotTarget.TARGET_NOT_REACHED_LABEL,
    val currentWeightLabel: String? = null,
    val captureElapsedLabel: String? = null,
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
        scaleModeLabel: String? = null,
        selectedScaleSource: CaptureScaleSource = CaptureScaleSource.FAKE,
        captureSourceStatusLabel: String = "Capture source: Fake scale/demo",
        captureSourceMessage: String? = null,
        isPrimaryActionEnabled: Boolean = true
    ): CaptureUiState = CaptureUiState(
        status = CaptureStatus.READY,
        scaleConnectionLabel = scaleConnectionLabel,
        scaleModeLabel = scaleModeLabel,
        selectedScaleSource = selectedScaleSource,
        captureSourceStatusLabel = captureSourceStatusLabel,
        captureSourceMessage = captureSourceMessage,
        shotStatusLabel = "Ready",
        primaryActionLabel = "Start capture",
        isPrimaryActionEnabled = isPrimaryActionEnabled
    )

    fun recording(
        scaleConnectionLabel: String = "Scale: Not connected",
        scaleModeLabel: String? = null,
        selectedScaleSource: CaptureScaleSource = CaptureScaleSource.FAKE,
        captureSourceStatusLabel: String = "Capture source: Fake scale/demo",
        captureSourceMessage: String? = null
    ): CaptureUiState = CaptureUiState(
        status = CaptureStatus.RECORDING,
        scaleConnectionLabel = scaleConnectionLabel,
        scaleModeLabel = scaleModeLabel,
        selectedScaleSource = selectedScaleSource,
        captureSourceStatusLabel = captureSourceStatusLabel,
        captureSourceMessage = captureSourceMessage,
        shotStatusLabel = "Recording",
        primaryActionLabel = "Stop & save",
        isPrimaryActionEnabled = true,
        currentWeightLabel = "Weight: 0.0 g",
        captureElapsedLabel = "Capture elapsed: 0 s",
        averageFlowLabel = "Average flow: 0.0 g/s"
    )

    fun savedConfirmation(
        scaleConnectionLabel: String = "Scale: Not connected",
        scaleModeLabel: String? = null,
        selectedScaleSource: CaptureScaleSource = CaptureScaleSource.FAKE,
        captureSourceStatusLabel: String = "Capture source: Fake scale/demo",
        captureSourceMessage: String? = null
    ): CaptureUiState = CaptureUiState(
        status = CaptureStatus.SAVED,
        scaleConnectionLabel = scaleConnectionLabel,
        scaleModeLabel = scaleModeLabel,
        selectedScaleSource = selectedScaleSource,
        captureSourceStatusLabel = captureSourceStatusLabel,
        captureSourceMessage = captureSourceMessage,
        shotStatusLabel = "Shot saved",
        primaryActionLabel = "Start capture",
        isPrimaryActionEnabled = false
    )
}
