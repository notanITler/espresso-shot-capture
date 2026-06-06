package com.example.espressoshotcapture.history

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressoshotcapture.EspressoShotCaptureApplication

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
    val isDebuggable = LocalContext.current.applicationInfo.flags and
        ApplicationInfo.FLAG_DEBUGGABLE != 0

    ShotHistoryScreen(
        uiState = uiState,
        onAddTestShot = if (isDebuggable) viewModel::addTestShot else null,
        modifier = modifier
    )
}

@Composable
fun ShotHistoryScreen(
    uiState: ShotHistoryUiState,
    onAddTestShot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ShotHistoryScreen(
        items = uiState.items,
        onAddTestShot = onAddTestShot,
        modifier = modifier
    )
}

@Composable
fun ShotHistoryScreen(
    items: List<ShotHistoryItem>,
    onAddTestShot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (onAddTestShot != null) {
            BasicText(
                text = "Add test shot",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddTestShot)
                    .padding(16.dp)
            )
        }

        if (items.isEmpty()) {
            BasicText(
                text = "No saved shots",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = items, key = { item -> item.id }) { item ->
                    ShotHistoryRow(item = item)
                }
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
