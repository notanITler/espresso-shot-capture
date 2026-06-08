package com.example.espressoshotcapture.history

data class ShotHistoryUiState(
    val items: List<ShotHistoryItem>,
    val selectedShotDetail: ShotHistoryDetail? = null
)

data class ShotHistoryDetail(
    val id: String,
    val createdAtEpochMillis: Long,
    val json: String,
    val finalYieldLabel: String = ShotHistoryMapper.UNKNOWN_YIELD_LABEL,
    val flowTimeLabel: String = ShotHistoryMapper.UNKNOWN_FLOW_TIME_LABEL,
    val targetYieldLabel: String = ShotHistoryMapper.UNKNOWN_TARGET_YIELD_LABEL,
    val averageFlowLabel: String = ShotHistoryMapper.UNKNOWN_AVERAGE_FLOW_LABEL,
    val targetReachedLabel: String = ShotHistoryMapper.UNKNOWN_TARGET_REACHED_LABEL
)
