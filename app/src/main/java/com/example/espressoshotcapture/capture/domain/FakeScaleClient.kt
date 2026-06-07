package com.example.espressoshotcapture.capture.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeScaleClient(
    private val readingSequence: List<ScaleReading> = defaultReadingSequence()
) : ScaleClient {
    init {
        require(readingSequence.isNotEmpty()) {
            "FakeScaleClient requires at least one scale reading."
        }
    }

    private val _connectionState = MutableStateFlow<ScaleConnectionState>(
        ScaleConnectionState.Disconnected
    )
    override val connectionState: Flow<ScaleConnectionState> =
        _connectionState.asStateFlow()

    private val _readings = MutableSharedFlow<ScaleReading>(replay = 1)
    override val readings: Flow<ScaleReading> = _readings.asSharedFlow()

    private var nextReadingIndex = 0

    override fun connect() {
        _connectionState.value = ScaleConnectionState.Connecting
        _connectionState.value = ScaleConnectionState.Connected
    }

    override fun disconnect() {
        _connectionState.value = ScaleConnectionState.Disconnected
    }

    fun emitNextReading(): Boolean {
        if (_connectionState.value != ScaleConnectionState.Connected) {
            return false
        }

        val reading = readingSequence[nextReadingIndex]
        nextReadingIndex = (nextReadingIndex + 1) % readingSequence.size
        return _readings.tryEmit(reading)
    }

    companion object {
        private fun defaultReadingSequence(): List<ScaleReading> =
            listOf(
                ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
                ScaleReading(timestampMillis = 100L, weightGrams = 0.4),
                ScaleReading(timestampMillis = 200L, weightGrams = 1.2),
                ScaleReading(timestampMillis = 300L, weightGrams = 2.1),
                ScaleReading(timestampMillis = 400L, weightGrams = 3.0)
            )
    }
}
