package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.capture.domain.TasteDirection

data class ShotHistoryUiState(
    val items: List<ShotHistoryItem>,
    val selectedShotDetail: ShotHistoryDetail? = null,
    val metadataEditor: ShotUserMetadataEditorState? = null,
    val beanFilterOptions: List<ShotHistoryBeanFilterOption> = listOf(
        ShotHistoryBeanFilterOption(
            key = ShotHistoryBeanFilterKeys.ALL,
            label = "All shots",
            isSelected = true
        )
    )
)

data class ShotHistoryBeanFilterOption(
    val key: String,
    val label: String,
    val isSelected: Boolean
)

object ShotHistoryBeanFilterKeys {
    const val ALL: String = "all"
    const val UNASSIGNED: String = "unassigned"

    fun bean(normalizedBeanName: String): String =
        "bean:$normalizedBeanName"
}

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

data class ShotUserMetadataEditorState(
    val shotId: String,
    val ratingText: String = "",
    val tasteDirection: TasteDirection? = null,
    val grindSetting: String = "",
    val beanName: String = "",
    val notes: String = "",
    val validationMessage: String? = null
)
