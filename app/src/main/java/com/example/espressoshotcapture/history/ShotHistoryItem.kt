package com.example.espressoshotcapture.history

data class ShotHistoryItem(
    val id: String,
    val createdAtEpochMillis: Long,
    val finalYieldLabel: String = ShotHistoryMapper.UNKNOWN_YIELD_LABEL,
    val flowTimeLabel: String = ShotHistoryMapper.UNKNOWN_FLOW_TIME_LABEL,
    val targetYieldLabel: String = ShotHistoryMapper.UNKNOWN_TARGET_YIELD_LABEL
)
