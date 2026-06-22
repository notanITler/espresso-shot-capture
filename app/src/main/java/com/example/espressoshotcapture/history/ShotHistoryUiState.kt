package com.example.espressoshotcapture.history

data class ShotHistoryUiState(
    val items: List<ShotHistoryItem>,
    val selectedShotDetail: ShotHistoryDetail? = null
)

data class ShotHistoryDetail(
    val id: String,
    val createdAtEpochMillis: Long,
    val json: String,
    val createdLabel: String = ShotHistoryMapper.createdLabel(createdAtEpochMillis),
    val sourceLabel: String = ShotHistoryMapper.UNKNOWN_SOURCE_LABEL,
    val qualityLabel: String = ShotHistoryMapper.UNKNOWN_QUALITY_LABEL,
    val finalYieldLabel: String = ShotHistoryMapper.UNKNOWN_YIELD_LABEL,
    val flowTimeLabel: String = ShotHistoryMapper.UNKNOWN_FLOW_TIME_LABEL,
    val sampleCountLabel: String = ShotHistoryMapper.UNKNOWN_SAMPLE_COUNT_LABEL,
    val doseLabel: String = ShotHistoryMapper.UNKNOWN_DOSE_LABEL,
    val targetYieldLabel: String = ShotHistoryMapper.UNKNOWN_TARGET_YIELD_LABEL,
    val ratioLabel: String = ShotHistoryMapper.UNKNOWN_RATIO_LABEL,
    val averageFlowLabel: String = ShotHistoryMapper.UNKNOWN_AVERAGE_FLOW_LABEL,
    val targetReachedLabel: String = ShotHistoryMapper.UNKNOWN_TARGET_REACHED_LABEL
)
