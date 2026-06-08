package com.example.espressoshotcapture.history

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication

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
    Column(modifier = modifier.fillMaxSize()) {
        BasicText(
            text = "Recent Shot History",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        )
        if (items.isEmpty()) {
            BasicText(
                text = "No saved shots",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            val visibleItems = items.take(MAX_VISIBLE_HISTORY_ROWS)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(items = visibleItems, key = { item -> item.id }) { item ->
                    ShotHistoryRow(
                        item = item,
                        onClick = { onShotSelected(item.id) }
                    )
                }
            }
            selectedShotDetail?.let { detail ->
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
private fun ShotHistoryDetailView(detail: ShotHistoryDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        BasicText(
            text = "Selected Shot Detail",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        )
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
                .padding(8.dp)
        ) {
            BasicText(text = detail.json)
        }
    }
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
