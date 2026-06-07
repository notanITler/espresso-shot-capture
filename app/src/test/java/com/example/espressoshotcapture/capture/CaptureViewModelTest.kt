package com.example.espressoshotcapture.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureViewModelTest {
    @Test
    fun initialStateComesFromInitialDisconnectedReadyMapper() {
        val viewModel = CaptureViewModel()

        assertEquals(
            CaptureUiStateMapper.initialDisconnectedReady(),
            viewModel.uiState.value
        )
    }
}
