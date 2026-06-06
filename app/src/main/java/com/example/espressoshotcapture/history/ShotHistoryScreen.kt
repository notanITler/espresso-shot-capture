package com.example.espressoshotcapture.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ShotHistoryRoute(
    viewModel: ShotHistoryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    ShotHistoryScreen(
        uiState = uiState,
        modifier = modifier
    )
}

@Composable
fun ShotHistoryScreen(
    uiState: ShotHistoryUiState,
    modifier: Modifier = Modifier
) {
    ShotHistoryScreen(
        items = uiState.items,
        modifier = modifier
    )
}

@Composable
fun ShotHistoryScreen(
    items: List<ShotHistoryItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        BasicText(
            text = "No saved shots",
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items = items, key = { item -> item.id }) { item ->
                ShotHistoryRow(item = item)
            }
        }
    }
}

@Composable
private fun ShotHistoryRow(item: ShotHistoryItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        BasicText(text = item.id)
        BasicText(text = item.createdAtEpochMillis.toString())
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
                ShotHistoryItem(id = "shot-1000", createdAtEpochMillis = 1_000L),
                ShotHistoryItem(id = "shot-2000", createdAtEpochMillis = 2_000L)
            )
        )
    )
}
