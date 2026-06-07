package com.example.espressoshotcapture.capture

data class CaptureUiState(
    val scaleConnectionLabel: String,
    val shotStatusLabel: String,
    val primaryActionLabel: String,
    val isPrimaryActionEnabled: Boolean
)

object CaptureUiStateMapper {
    fun initialDisconnectedReady(): CaptureUiState = CaptureUiState(
        scaleConnectionLabel = "Scale: Not connected",
        shotStatusLabel = "Ready",
        primaryActionLabel = "Start capture",
        isPrimaryActionEnabled = true
    )
}
