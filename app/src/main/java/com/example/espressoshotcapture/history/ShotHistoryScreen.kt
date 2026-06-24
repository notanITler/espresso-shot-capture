package com.example.espressoshotcapture.history

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication
import com.example.espressoshotcapture.capture.domain.TasteDirection
import com.example.espressoshotcapture.ui.SectionContainer

private const val HISTORY_PREVIEW_ROW_COUNT = 3

@Composable
fun ShotHistoryRoute(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as EspressoShotCaptureApplication
    val viewModel: ShotHistoryViewModel = viewModel(
        factory = ShotHistoryViewModel.factory(application.appContainer.shotRepository)
    )

    ShotHistoryRoute(
        viewModel = viewModel,
        modifier = modifier
    )
}

@Composable
fun ShotHistoryRoute(
    viewModel: ShotHistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    ShotHistoryScreen(
        uiState = uiState,
        onShotSelected = viewModel::selectShot,
        onBeanFilterSelected = viewModel::selectBeanFilter,
        onMetadataRatingChange = viewModel::updateMetadataRating,
        onMetadataTasteDirectionChange = viewModel::updateMetadataTasteDirection,
        onMetadataGrindSettingChange = viewModel::updateMetadataGrindSetting,
        onMetadataBeanNameChange = viewModel::updateMetadataBeanName,
        onMetadataNotesChange = viewModel::updateMetadataNotes,
        onMetadataSave = viewModel::saveShotUserMetadata,
        onMetadataClear = viewModel::clearShotUserMetadata,
        modifier = modifier
    )
}

@Composable
fun ShotHistoryScreen(
    uiState: ShotHistoryUiState,
    onShotSelected: (String) -> Unit = {},
    onBeanFilterSelected: (String) -> Unit = {},
    onMetadataRatingChange: (String) -> Unit = {},
    onMetadataTasteDirectionChange: (TasteDirection?) -> Unit = {},
    onMetadataGrindSettingChange: (String) -> Unit = {},
    onMetadataBeanNameChange: (String) -> Unit = {},
    onMetadataNotesChange: (String) -> Unit = {},
    onMetadataSave: () -> Unit = {},
    onMetadataClear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ShotHistoryScreen(
        items = uiState.items,
        beanFilterOptions = uiState.beanFilterOptions,
        selectedShotDetail = uiState.selectedShotDetail,
        metadataEditor = uiState.metadataEditor,
        onShotSelected = onShotSelected,
        onBeanFilterSelected = onBeanFilterSelected,
        onMetadataRatingChange = onMetadataRatingChange,
        onMetadataTasteDirectionChange = onMetadataTasteDirectionChange,
        onMetadataGrindSettingChange = onMetadataGrindSettingChange,
        onMetadataBeanNameChange = onMetadataBeanNameChange,
        onMetadataNotesChange = onMetadataNotesChange,
        onMetadataSave = onMetadataSave,
        onMetadataClear = onMetadataClear,
        modifier = modifier
    )
}

@Composable
fun ShotHistoryScreen(
    items: List<ShotHistoryItem>,
    beanFilterOptions: List<ShotHistoryBeanFilterOption> = listOf(
        ShotHistoryBeanFilterOption(
            key = ShotHistoryBeanFilterKeys.ALL,
            label = "All shots",
            isSelected = true
        )
    ),
    selectedShotDetail: ShotHistoryDetail? = null,
    metadataEditor: ShotUserMetadataEditorState? = selectedShotDetail?.let { detail ->
        ShotUserMetadataEditorState(shotId = detail.id)
    },
    onShotSelected: (String) -> Unit = {},
    onBeanFilterSelected: (String) -> Unit = {},
    onMetadataRatingChange: (String) -> Unit = {},
    onMetadataTasteDirectionChange: (TasteDirection?) -> Unit = {},
    onMetadataGrindSettingChange: (String) -> Unit = {},
    onMetadataBeanNameChange: (String) -> Unit = {},
    onMetadataNotesChange: (String) -> Unit = {},
    onMetadataSave: () -> Unit = {},
    onMetadataClear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isHistoryExpanded by remember { mutableStateOf(false) }
    val visibleItems = if (isHistoryExpanded) {
        items
    } else {
        items.take(HISTORY_PREVIEW_ROW_COUNT)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionContainer(title = "Recent Shot History") {
            BeanFilterBar(
                options = beanFilterOptions,
                onBeanFilterSelected = { key ->
                    isHistoryExpanded = false
                    onBeanFilterSelected(key)
                }
            )
            if (items.isEmpty()) {
                BasicText(
                    text = "No saved shots",
                    modifier = Modifier.padding(top = 8.dp),
                    style = historyMutedStyle()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ShotHistoryScreenTestTags.HISTORY_LIST)
                ) {
                    visibleItems.forEach { item ->
                        ShotHistoryRow(
                            item = item,
                            onClick = { onShotSelected(item.id) }
                        )
                    }
                }
                if (items.size > HISTORY_PREVIEW_ROW_COUNT) {
                    BasicText(
                        text = if (isHistoryExpanded) "Show fewer" else "View all history",
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag(ShotHistoryScreenTestTags.HISTORY_EXPAND_ACTION)
                            .clickable {
                                isHistoryExpanded = !isHistoryExpanded
                            },
                        style = historyActionStyle()
                    )
                }
            }
        }
        ShotHistoryDetailSection(
            detail = selectedShotDetail,
            metadataEditor = metadataEditor,
            onMetadataRatingChange = onMetadataRatingChange,
            onMetadataTasteDirectionChange = onMetadataTasteDirectionChange,
            onMetadataGrindSettingChange = onMetadataGrindSettingChange,
            onMetadataBeanNameChange = onMetadataBeanNameChange,
            onMetadataNotesChange = onMetadataNotesChange,
            onMetadataSave = onMetadataSave,
            onMetadataClear = onMetadataClear
        )
    }
}

@Composable
private fun BeanFilterBar(
    options: List<ShotHistoryBeanFilterOption>,
    onBeanFilterSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ShotHistoryScreenTestTags.BEAN_FILTER_SECTION)
    ) {
        BasicText(
            text = "Bean",
            style = historySectionLabelStyle()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                BeanFilterChip(
                    option = option,
                    onClick = { onBeanFilterSelected(option.key) },
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
private fun BeanFilterChip(
    option: ShotHistoryBeanFilterOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = option.label,
        modifier = modifier
            .testTag(ShotHistoryScreenTestTags.beanFilterOption(option.key))
            .clickable(onClick = onClick)
            .background(
                color = if (option.isSelected) Color(0xFF244033) else Color(0xFF0F1114),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = if (option.isSelected) Color(0xFF5DCB8A) else Color(0xFF3A414A),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        style = historyBodyStyle()
    )
}

@Composable
private fun ShotHistoryDetailSection(
    detail: ShotHistoryDetail?,
    metadataEditor: ShotUserMetadataEditorState?,
    onMetadataRatingChange: (String) -> Unit,
    onMetadataTasteDirectionChange: (TasteDirection?) -> Unit,
    onMetadataGrindSettingChange: (String) -> Unit,
    onMetadataBeanNameChange: (String) -> Unit,
    onMetadataNotesChange: (String) -> Unit,
    onMetadataSave: () -> Unit,
    onMetadataClear: () -> Unit
) {
    SectionContainer(title = "Selected Shot Detail") {
        if (detail == null) {
            BasicText(
                text = "Select a saved shot to inspect it",
                modifier = Modifier.padding(top = 8.dp),
                style = historyMutedStyle()
            )
        } else {
            Box(modifier = Modifier.testTag(ShotHistoryScreenTestTags.SELECTED_DETAIL)) {
                ShotHistoryDetailView(
                    detail = detail,
                    metadataEditor = metadataEditor,
                    onMetadataRatingChange = onMetadataRatingChange,
                    onMetadataTasteDirectionChange = onMetadataTasteDirectionChange,
                    onMetadataGrindSettingChange = onMetadataGrindSettingChange,
                    onMetadataBeanNameChange = onMetadataBeanNameChange,
                    onMetadataNotesChange = onMetadataNotesChange,
                    onMetadataSave = onMetadataSave,
                    onMetadataClear = onMetadataClear
                )
            }
        }
    }
}

@Composable
private fun ShotHistoryRow(
    item: ShotHistoryItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ShotHistoryScreenTestTags.historyRow(item.id))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp)
            .background(
                color = Color(0xFF151A20),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF28303A),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(44.dp)
                .background(
                    color = if (item.comparisonTitleLabel == "Unassigned bean") {
                        Color(0xFFF2C94C)
                    } else {
                        Color(0xFF5DCB8A)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.comparisonTitleLabel,
                modifier = Modifier.testTag(ShotHistoryScreenTestTags.historyRowTitle(item.id)),
                style = historyStrongStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.comparisonMetadataLabel != null) {
                BasicText(
                    text = item.comparisonMetadataLabel,
                    modifier = Modifier.testTag(ShotHistoryScreenTestTags.historyRowMetadata(item.id)),
                    style = historyBodyStyle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            BasicText(
                text = item.comparisonMetricsLabel,
                modifier = Modifier.testTag(ShotHistoryScreenTestTags.historyRowMetrics(item.id)),
                style = historyMutedStyle(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFF151A20),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF28303A),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        val displayValue = value.removeLabelPrefix()
        BasicText(
            text = label,
            style = historyMutedStyle()
        )
        BasicText(
            text = displayValue,
            style = historyStrongStyle()
        )
    }
}

private fun String.removeLabelPrefix(): String =
    substringAfter(": ", this)

@Composable
private fun ShotHistoryDetailView(
    detail: ShotHistoryDetail,
    metadataEditor: ShotUserMetadataEditorState?,
    onMetadataRatingChange: (String) -> Unit,
    onMetadataTasteDirectionChange: (TasteDirection?) -> Unit,
    onMetadataGrindSettingChange: (String) -> Unit,
    onMetadataBeanNameChange: (String) -> Unit,
    onMetadataNotesChange: (String) -> Unit,
    onMetadataSave: () -> Unit,
    onMetadataClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRawJsonVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ShotHistoryScreenTestTags.DETAIL_REPORT)
            .padding(top = 8.dp)
    ) {
        DetailGroup(
            title = "Main result summary",
            modifier = Modifier.testTag(ShotHistoryScreenTestTags.DETAIL_MAIN_SUMMARY)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailMetricCard(label = "Dose", value = detail.doseLabel, modifier = Modifier.weight(1f))
                DetailMetricCard(label = "Yield", value = detail.finalYieldLabel, modifier = Modifier.weight(1f))
                DetailMetricCard(label = "Average flow", value = detail.averageFlowLabel, modifier = Modifier.weight(1f))
            }
        }
        DetailGroup(
            title = "Shot timing / target",
            modifier = Modifier.testTag(ShotHistoryScreenTestTags.DETAIL_TIMING_TARGET)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailMetricCard(label = "Flow time", value = detail.flowTimeLabel, modifier = Modifier.weight(1f))
                DetailMetricCard(label = "Target", value = detail.targetYieldLabel, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailMetricCard(label = "Ratio", value = detail.ratioLabel, modifier = Modifier.weight(1f))
                DetailMetricCard(label = "Target reached", value = detail.targetReachedLabel, modifier = Modifier.weight(1f))
            }
        }
        DetailGroup(
            title = "Data confidence",
            modifier = Modifier.testTag(ShotHistoryScreenTestTags.DETAIL_DATA_CONFIDENCE)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailMetricCard(label = "Source", value = detail.sourceLabel, modifier = Modifier.weight(1f))
                DetailMetricCard(label = "Data status", value = detail.qualityLabel, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailMetricCard(label = "Weight readings", value = detail.sampleCountLabel, modifier = Modifier.weight(1f))
                DetailMetricCard(label = "Created", value = detail.createdLabel, modifier = Modifier.weight(1f))
            }
        }
        if (metadataEditor != null) {
            ShotMetadataEditor(
                editor = metadataEditor,
                onRatingChange = onMetadataRatingChange,
                onTasteDirectionChange = onMetadataTasteDirectionChange,
                onGrindSettingChange = onMetadataGrindSettingChange,
                onBeanNameChange = onMetadataBeanNameChange,
                onNotesChange = onMetadataNotesChange,
                onSave = onMetadataSave,
                onClear = onMetadataClear
            )
        }
        BasicText(
            text = if (isRawJsonVisible) {
                "Hide raw JSON / debug detail"
            } else {
                "Show raw JSON / debug detail"
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(ShotHistoryScreenTestTags.RAW_JSON_TOGGLE)
                .clickable { isRawJsonVisible = !isRawJsonVisible },
            style = historyActionStyle()
        )
        if (isRawJsonVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .padding(top = 8.dp)
                    .background(
                        color = Color(0xFF0F1114),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF3A414A),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .verticalScroll(rememberScrollState())
                    .testTag(ShotHistoryScreenTestTags.RAW_JSON)
                    .padding(8.dp)
            ) {
                BasicText(
                    text = detail.json,
                    style = historyMonoStyle()
                )
            }
        }
    }
}

@Composable
private fun ShotMetadataEditor(
    editor: ShotUserMetadataEditorState,
    onRatingChange: (String) -> Unit,
    onTasteDirectionChange: (TasteDirection?) -> Unit,
    onGrindSettingChange: (String) -> Unit,
    onBeanNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    DetailGroup(
        title = "Shot feedback",
        modifier = Modifier.testTag(ShotHistoryScreenTestTags.METADATA_EDITOR)
    ) {
        MetadataInput(
            label = "Rating 1-5",
            value = editor.ratingText,
            onValueChange = onRatingChange,
            testTag = ShotHistoryScreenTestTags.METADATA_RATING_INPUT
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(text = "Taste direction", style = historyMutedStyle())
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TasteDirectionChip(
                label = "None",
                selected = editor.tasteDirection == null,
                testTag = ShotHistoryScreenTestTags.METADATA_TASTE_NONE,
                onClick = { onTasteDirectionChange(null) },
                modifier = Modifier.weight(1f)
            )
            TasteDirectionChip(
                label = "Sour",
                selected = editor.tasteDirection == TasteDirection.SOUR,
                testTag = ShotHistoryScreenTestTags.METADATA_TASTE_SOUR,
                onClick = { onTasteDirectionChange(TasteDirection.SOUR) },
                modifier = Modifier.weight(1f)
            )
            TasteDirectionChip(
                label = "Balanced",
                selected = editor.tasteDirection == TasteDirection.BALANCED,
                testTag = ShotHistoryScreenTestTags.METADATA_TASTE_BALANCED,
                onClick = { onTasteDirectionChange(TasteDirection.BALANCED) },
                modifier = Modifier.weight(1f)
            )
            TasteDirectionChip(
                label = "Bitter",
                selected = editor.tasteDirection == TasteDirection.BITTER,
                testTag = ShotHistoryScreenTestTags.METADATA_TASTE_BITTER,
                onClick = { onTasteDirectionChange(TasteDirection.BITTER) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        MetadataInput(
            label = "Grind setting",
            value = editor.grindSetting,
            onValueChange = onGrindSettingChange,
            testTag = ShotHistoryScreenTestTags.METADATA_GRIND_INPUT
        )
        Spacer(modifier = Modifier.height(8.dp))
        MetadataInput(
            label = "Bean name",
            value = editor.beanName,
            onValueChange = onBeanNameChange,
            testTag = ShotHistoryScreenTestTags.METADATA_BEAN_INPUT
        )
        Spacer(modifier = Modifier.height(8.dp))
        MetadataInput(
            label = "Notes",
            value = editor.notes,
            onValueChange = onNotesChange,
            testTag = ShotHistoryScreenTestTags.METADATA_NOTES_INPUT,
            singleLine = false
        )
        if (editor.validationMessage != null) {
            BasicText(
                text = editor.validationMessage,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(ShotHistoryScreenTestTags.METADATA_VALIDATION_MESSAGE),
                style = historyMutedStyle()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetadataAction(
                text = "Save feedback",
                modifier = Modifier
                    .weight(1f)
                    .testTag(ShotHistoryScreenTestTags.METADATA_SAVE_ACTION)
                    .clickable(onClick = onSave)
            )
            MetadataAction(
                text = "Clear",
                modifier = Modifier
                    .weight(1f)
                    .testTag(ShotHistoryScreenTestTags.METADATA_CLEAR_ACTION)
                    .clickable(onClick = onClear)
            )
        }
    }
}

@Composable
private fun MetadataInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    singleLine: Boolean = true
) {
    Column {
        BasicText(text = label, style = historyMutedStyle())
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = historyStrongStyle(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .background(
                    color = Color(0xFF0F1114),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF3A414A),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun TasteDirectionChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = label,
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
            .background(
                color = if (selected) Color(0xFF244033) else Color(0xFF0F1114),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF5DCB8A) else Color(0xFF3A414A),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        style = historyBodyStyle()
    )
}

@Composable
private fun MetadataAction(
    text: String,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = text,
        modifier = modifier
            .background(
                color = Color(0xFF24282E),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF3A414A),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        style = historyActionStyle()
    )
}

@Composable
private fun DetailGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(
                color = Color(0xFF20242A),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF30363D),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(10.dp)
    ) {
        BasicText(
            text = title,
            style = historySectionLabelStyle()
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun DetailMetricRow(
    primary: String,
    secondary: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        BasicText(
            text = primary,
            modifier = Modifier.weight(1f),
            style = historyStrongStyle()
        )
        if (secondary != null) {
            BasicText(
                text = secondary,
                modifier = Modifier.weight(1f),
                style = historyBodyStyle()
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun historyStrongStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFF6F7F9),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )

private fun historyBodyStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFE6EBF0),
        fontSize = 13.sp
    )

private fun historyMutedStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFAAB2BC),
        fontSize = 12.sp
    )

private fun historySectionLabelStyle(): TextStyle =
    TextStyle(
        color = Color(0xFF97A2AD),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )

private fun historyActionStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFF2C94C),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )

private fun historyMonoStyle(): TextStyle =
    TextStyle(
        color = Color(0xFFE6EBF0),
        fontSize = 11.sp
    )

object ShotHistoryScreenTestTags {
    const val BEAN_FILTER_SECTION = "ShotHistoryBeanFilter"
    const val HISTORY_LIST = "ShotHistoryList"
    const val HISTORY_EXPAND_ACTION = "ShotHistoryExpandAction"
    const val SELECTED_DETAIL = "SelectedShotDetail"
    const val DETAIL_REPORT = "ShotDetailReport"
    const val DETAIL_MAIN_SUMMARY = "ShotDetailMainSummary"
    const val DETAIL_TIMING_TARGET = "ShotDetailTimingTarget"
    const val DETAIL_DATA_CONFIDENCE = "ShotDetailDataConfidence"
    const val RAW_JSON_TOGGLE = "ShotDetailRawJsonToggle"
    const val RAW_JSON = "ShotDetailRawJsonContent"
    const val METADATA_EDITOR = "ShotFeedbackSection"
    const val METADATA_RATING_INPUT = "ShotFeedbackRating"
    const val METADATA_TASTE_NONE = "ShotFeedbackTasteNone"
    const val METADATA_TASTE_SOUR = "ShotFeedbackTasteSour"
    const val METADATA_TASTE_BALANCED = "ShotFeedbackTasteBalanced"
    const val METADATA_TASTE_BITTER = "ShotFeedbackTasteBitter"
    const val METADATA_GRIND_INPUT = "ShotFeedbackGrindSetting"
    const val METADATA_BEAN_INPUT = "ShotFeedbackBeanName"
    const val METADATA_NOTES_INPUT = "ShotFeedbackNotes"
    const val METADATA_SAVE_ACTION = "ShotFeedbackSave"
    const val METADATA_CLEAR_ACTION = "ShotFeedbackClear"
    const val METADATA_VALIDATION_MESSAGE = "ShotFeedbackValidationMessage"

    fun beanFilterOption(key: String): String = "ShotHistoryBeanFilter_$key"
    fun historyRow(id: String): String = "ShotHistoryRow_$id"
    fun historyRowTitle(id: String): String = "ShotHistoryRowTitle_$id"
    fun historyRowMetadata(id: String): String = "ShotHistoryRowMetadata_$id"
    fun historyRowMetrics(id: String): String = "ShotHistoryRowMetrics_$id"
}

@Preview
@Composable
private fun EmptyShotHistoryScreenPreview() {
    ShotHistoryScreen(uiState = ShotHistoryUiState(items = emptyList()))
}

@Preview
@Composable
private fun PopulatedShotHistoryScreenPreview() {
    ShotHistoryScreen(
        uiState = ShotHistoryUiState(
            items = listOf(
                ShotHistoryItem(
                    id = "shot-1000",
                    createdAtEpochMillis = 1_000L,
                    finalYieldLabel = "Yield: 36.8 g",
                    flowTimeLabel = "Flow time: 28 s",
                    targetYieldLabel = "Target: 36.0 g",
                    sourceLabel = "Source: Fake/demo",
                    qualityLabel = "Data status: Complete",
                    sampleCountLabel = "Weight readings: 3",
                    doseLabel = "Dose: 18.0 g",
                    ratioLabel = "Ratio: 1:2"
                ),
                ShotHistoryItem(
                    id = "shot-2000",
                    createdAtEpochMillis = 2_000L,
                    finalYieldLabel = "Yield: 37.2 g",
                    flowTimeLabel = "Flow time: 29 s",
                    targetYieldLabel = "Target: 36.0 g",
                    sourceLabel = "Source: Decent Scale",
                    qualityLabel = "Data status: Complete",
                    sampleCountLabel = "Weight readings: 4",
                    doseLabel = "Dose: 18.0 g",
                    ratioLabel = "Ratio: 1:2"
                )
            ),
            selectedShotDetail = ShotHistoryDetail(
                id = "shot-2000",
                createdAtEpochMillis = 2_000L,
                json = """{"schemaVersion":1,"shot":{"id":"shot-2000"}}""",
                sourceLabel = "Source: Decent Scale",
                qualityLabel = "Data status: Complete",
                finalYieldLabel = "Yield: 37.2 g",
                flowTimeLabel = "Flow time: 29 s",
                targetYieldLabel = "Target: 36.0 g",
                averageFlowLabel = "Average flow: --",
                sampleCountLabel = "Weight readings: 4",
                doseLabel = "Dose: 18.0 g",
                ratioLabel = "Ratio: 1:2",
                targetReachedLabel = "Target reached: yes"
            )
        )
    )
}
