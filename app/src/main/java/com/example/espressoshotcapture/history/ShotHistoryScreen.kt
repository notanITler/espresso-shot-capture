package com.example.espressoshotcapture.history

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication
import com.example.espressoshotcapture.ui.SectionContainer

private const val MAX_VISIBLE_HISTORY_ROWS = 5

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
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                val visibleItems = items.take(MAX_VISIBLE_HISTORY_ROWS)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
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
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            ShotHistoryDetailView(
                detail = detail,
                modifier = Modifier.testTag(ShotHistoryScreenTestTags.SELECTED_DETAIL)
            )
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
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        BasicText(text = item.id)
        BasicText(
            text = "${item.finalYieldLabel}  |  ${item.flowTimeLabel}  |  ${item.targetYieldLabel}"
        )
    }
}

@Composable
private fun ShotHistoryDetailView(
    detail: ShotHistoryDetail,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        BasicText(text = "id: ${detail.id}")
        BasicText(text = "createdAtEpochMillis: ${detail.createdAtEpochMillis}")
        BasicText(text = detail.finalYieldLabel)
        BasicText(text = detail.flowTimeLabel)
        BasicText(text = detail.averageFlowLabel)
        BasicText(text = detail.targetYieldLabel)
        BasicText(text = detail.targetReachedLabel)
        BasicText(
            text = "Raw JSON / debug detail",
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(fontWeight = FontWeight.SemiBold)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .padding(top = 8.dp)
                .border(width = 1.dp, color = Color.LightGray)
                .verticalScroll(rememberScrollState())
                .testTag(ShotHistoryScreenTestTags.RAW_JSON)
                .padding(8.dp)
        ) {
            BasicText(text = detail.json)
        }
    }
}

object ShotHistoryScreenTestTags {
    const val HISTORY_LIST = "history-list"
    const val SELECTED_DETAIL = "selected-shot-detail"
    const val RAW_JSON = "raw-json"
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
                    targetYieldLabel = "Target: 36.0 g"
                ),
                ShotHistoryItem(
                    id = "shot-2000",
                    createdAtEpochMillis = 2_000L,
                    finalYieldLabel = "Yield: 37.2 g",
                    flowTimeLabel = "Flow time: 29 s",
                    targetYieldLabel = "Target: 36.0 g"
                )
            ),
            selectedShotDetail = ShotHistoryDetail(
                id = "shot-2000",
                createdAtEpochMillis = 2_000L,
                json = """{"schemaVersion":1,"shot":{"id":"shot-2000"}}""",
                finalYieldLabel = "Yield: 37.2 g",
                flowTimeLabel = "Flow time: 29 s",
                targetYieldLabel = "Target: 36.0 g",
                averageFlowLabel = "Average flow: --",
                targetReachedLabel = "Target reached: yes"
            )
        )
    )
}
