package com.example.espressoshotcapture.history

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication
import com.example.espressoshotcapture.ui.SectionContainer

private const val MAX_VISIBLE_HISTORY_ROWS = 3

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
        modifier = modifier
    )
}

@Composable
fun ShotHistoryScreen(
    uiState: ShotHistoryUiState,
    onShotSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ShotHistoryScreen(
        items = uiState.items,
        selectedShotDetail = uiState.selectedShotDetail,
        onShotSelected = onShotSelected,
        modifier = modifier
    )
}

@Composable
fun ShotHistoryScreen(
    items: List<ShotHistoryItem>,
    selectedShotDetail: ShotHistoryDetail? = null,
    onShotSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionContainer(title = "Recent Shot History") {
            if (items.isEmpty()) {
                BasicText(
                    text = "No saved shots",
                    modifier = Modifier.padding(top = 8.dp),
                    style = historyMutedStyle()
                )
            } else {
                val visibleItems = items.take(MAX_VISIBLE_HISTORY_ROWS)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .testTag(ShotHistoryScreenTestTags.HISTORY_LIST)
                ) {
                    items(items = visibleItems, key = { item -> item.id }) { item ->
                        ShotHistoryRow(
                            item = item,
                            onClick = { onShotSelected(item.id) }
                        )
                    }
                }
            }
        }
        ShotHistoryDetailSection(detail = selectedShotDetail)
    }
}

@Composable
private fun ShotHistoryDetailSection(detail: ShotHistoryDetail?) {
    SectionContainer(title = "Selected Shot Detail") {
        if (detail == null) {
            BasicText(
                text = "Select a saved shot to inspect it",
                modifier = Modifier.padding(top = 8.dp),
                style = historyMutedStyle()
            )
        } else {
            Box(modifier = Modifier.testTag(ShotHistoryScreenTestTags.SELECTED_DETAIL)) {
                ShotHistoryDetailView(detail = detail)
            }
        }
    }
}

@Composable
private fun ShotHistoryRow(
    item: ShotHistoryItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ShotHistoryScreenTestTags.historyRow(item.id))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .background(
                color = Color(0xFF20242A),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF30363D),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = "${item.sourceLabel}  |  ${item.finalYieldLabel}",
            style = historyStrongStyle()
        )
        BasicText(
            text = "${item.flowTimeLabel}  |  ${item.sampleCountLabel}",
            style = historyBodyStyle()
        )
        BasicText(
            text = item.qualityLabel,
            style = historyMutedStyle()
        )
        BasicText(
            text = "Created: ${item.createdAtEpochMillis}",
            style = historyMutedStyle()
        )
    }
}

@Composable
private fun ShotHistoryDetailView(
    detail: ShotHistoryDetail,
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
            DetailMetricRow(primary = detail.doseLabel, secondary = detail.finalYieldLabel)
            DetailMetricRow(primary = detail.averageFlowLabel, secondary = "id: ${detail.id}")
        }
        DetailGroup(
            title = "Shot timing / target",
            modifier = Modifier.testTag(ShotHistoryScreenTestTags.DETAIL_TIMING_TARGET)
        ) {
            DetailMetricRow(primary = detail.flowTimeLabel, secondary = detail.targetYieldLabel)
            DetailMetricRow(primary = detail.ratioLabel, secondary = detail.targetReachedLabel)
        }
        DetailGroup(
            title = "Data confidence",
            modifier = Modifier.testTag(ShotHistoryScreenTestTags.DETAIL_DATA_CONFIDENCE)
        ) {
            DetailMetricRow(primary = detail.sourceLabel, secondary = detail.qualityLabel)
            DetailMetricRow(primary = detail.sampleCountLabel, secondary = "Created: ${detail.createdAtEpochMillis}")
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
    secondary: String
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
        BasicText(
            text = secondary,
            modifier = Modifier.weight(1f),
            style = historyBodyStyle()
        )
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
    const val HISTORY_LIST = "ShotHistoryList"
    const val SELECTED_DETAIL = "SelectedShotDetail"
    const val DETAIL_REPORT = "ShotDetailReport"
    const val DETAIL_MAIN_SUMMARY = "ShotDetailMainSummary"
    const val DETAIL_TIMING_TARGET = "ShotDetailTimingTarget"
    const val DETAIL_DATA_CONFIDENCE = "ShotDetailDataConfidence"
    const val RAW_JSON_TOGGLE = "ShotDetailRawJsonToggle"
    const val RAW_JSON = "ShotDetailRawJsonContent"

    fun historyRow(id: String): String = "ShotHistoryRow_$id"
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
