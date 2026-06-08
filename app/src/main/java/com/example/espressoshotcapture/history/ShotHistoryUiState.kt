package com.example.espressoshotcapture.history

data class ShotHistoryUiState(
    val items: List<ShotHistoryItem>,
    val selectedShotDetail: ShotHistoryDetail? = null
)

data class ShotHistoryDetail(
    val id: String,
    val createdAtEpochMillis: Long,
    val json: String
)
