package com.example.espressoshotcapture.history

import com.example.espressoshotcapture.persistence.ShotEntity
import com.example.espressoshotcapture.repository.ShotEntityMapper

object ShotHistoryStateMapper {
    fun fromEntities(
        entities: List<ShotEntity>,
        selectedShotId: String? = null,
        selectedBeanFilterKey: String = ShotHistoryBeanFilterKeys.ALL
    ): ShotHistoryUiState {
        val filterOptions = beanFilterOptions(
            entities = entities,
            selectedBeanFilterKey = selectedBeanFilterKey
        )
        val effectiveFilterKey = filterOptions
            .firstOrNull { option -> option.isSelected }
            ?.key
            ?: ShotHistoryBeanFilterKeys.ALL
        val filteredEntities = entities.filterByBeanFilter(effectiveFilterKey)
        val selectedEntity = filteredEntities.firstOrNull { entity -> entity.id == selectedShotId }
        return ShotHistoryUiState(
            items = ShotHistoryMapper.fromEntities(filteredEntities),
            selectedShotDetail = selectedEntity?.toDetail(),
            metadataEditor = selectedEntity?.toMetadataEditor(),
            beanFilterOptions = filterOptions
        )
    }

    private fun beanFilterOptions(
        entities: List<ShotEntity>,
        selectedBeanFilterKey: String
    ): List<ShotHistoryBeanFilterOption> {
        val beanGroups = entities
            .mapNotNull { entity ->
                entity.normalizedBeanName()?.let { key -> key to entity.beanName.orEmpty().trim() }
            }
            .groupBy(keySelector = { pair -> pair.first }, valueTransform = { pair -> pair.second })
            .mapValues { (_, displayNames) -> displayNames.first() }
            .toSortedMap()
        val hasUnassigned = entities.any { entity -> entity.normalizedBeanName() == null }
        val validKeys = buildSet {
            add(ShotHistoryBeanFilterKeys.ALL)
            if (hasUnassigned) add(ShotHistoryBeanFilterKeys.UNASSIGNED)
            beanGroups.keys.forEach { normalizedBean -> add(ShotHistoryBeanFilterKeys.bean(normalizedBean)) }
        }
        val effectiveSelectedKey = selectedBeanFilterKey.takeIf { key -> key in validKeys }
            ?: ShotHistoryBeanFilterKeys.ALL

        return buildList {
            add(
                ShotHistoryBeanFilterOption(
                    key = ShotHistoryBeanFilterKeys.ALL,
                    label = "All shots",
                    isSelected = effectiveSelectedKey == ShotHistoryBeanFilterKeys.ALL
                )
            )
            if (hasUnassigned) {
                add(
                    ShotHistoryBeanFilterOption(
                        key = ShotHistoryBeanFilterKeys.UNASSIGNED,
                        label = "Unassigned",
                        isSelected = effectiveSelectedKey == ShotHistoryBeanFilterKeys.UNASSIGNED
                    )
                )
            }
            beanGroups.forEach { (normalizedBean, displayName) ->
                val key = ShotHistoryBeanFilterKeys.bean(normalizedBean)
                add(
                    ShotHistoryBeanFilterOption(
                        key = key,
                        label = displayName,
                        isSelected = effectiveSelectedKey == key
                    )
                )
            }
        }
    }

    private fun List<ShotEntity>.filterByBeanFilter(filterKey: String): List<ShotEntity> =
        when (filterKey) {
            ShotHistoryBeanFilterKeys.ALL -> this
            ShotHistoryBeanFilterKeys.UNASSIGNED ->
                filter { entity -> entity.normalizedBeanName() == null }
            else -> {
                val beanKey = filterKey.removePrefix("bean:")
                filter { entity -> entity.normalizedBeanName() == beanKey }
            }
        }

    private fun ShotEntity.normalizedBeanName(): String? =
        ShotHistoryBeanFilterKeys.normalizedBeanName(beanName)

    private fun ShotEntity.toMetadataEditor(): ShotUserMetadataEditorState {
        val metadata = ShotEntityMapper.toUserMetadata(this)
        return ShotUserMetadataEditorState(
            shotId = id,
            ratingText = metadata.rating?.toString().orEmpty(),
            tasteDirection = metadata.tasteDirection,
            grindSetting = metadata.grindSetting.orEmpty(),
            beanName = metadata.beanName.orEmpty(),
            notes = metadata.notes.orEmpty()
        )
    }

    private fun ShotEntity.toDetail(): ShotHistoryDetail =
        ShotHistoryMapper.summaryFromJson(json).let { summary ->
            ShotHistoryDetail(
                id = id,
                createdAtEpochMillis = createdAtEpochMillis,
                json = json,
                createdLabel = ShotHistoryMapper.createdLabel(createdAtEpochMillis),
                sourceLabel = summary.sourceLabel,
                qualityLabel = summary.qualityLabel,
                finalYieldLabel = summary.finalYieldLabel,
                flowTimeLabel = summary.flowTimeLabel,
                sampleCountLabel = summary.sampleCountLabel,
                doseLabel = summary.doseLabel,
                targetYieldLabel = summary.targetYieldLabel,
                ratioLabel = summary.ratioLabel,
                averageFlowLabel = summary.averageFlowLabel,
                targetReachedLabel = summary.targetReachedLabel
            )
        }
}
