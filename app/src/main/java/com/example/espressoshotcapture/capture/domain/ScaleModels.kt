package com.example.espressoshotcapture.capture.domain

import kotlinx.coroutines.flow.Flow

data class ScaleReading(
    val timestampMillis: Long,
    val weightGrams: Double
)

sealed interface ScaleConnectionState {
    data object Disconnected : ScaleConnectionState
    data object Connecting : ScaleConnectionState
    data object Connected : ScaleConnectionState
    data class Error(val message: String) : ScaleConnectionState
}

interface ScaleClient {
    val connectionState: Flow<ScaleConnectionState>
    val readings: Flow<ScaleReading>

    fun connect()
    fun disconnect()
}
