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

    fun resetReadings() {
        nextReadingIndex = 0
    }

    fun emitNextReading(): Boolean {
        if (_connectionState.value != ScaleConnectionState.Connected) {
            return false
        }

        val reading = nextReading()
        return _readings.tryEmit(reading)
    }

    private fun nextReading(): ScaleReading {
        val sequenceIndex = nextReadingIndex
        nextReadingIndex += 1

        if (sequenceIndex < readingSequence.size) {
            return readingSequence[sequenceIndex]
        }

        val finalReading = readingSequence.last()
        val extraIndex = sequenceIndex - readingSequence.lastIndex
        return ScaleReading(
            timestampMillis = finalReading.timestampMillis + extraIndex * 1_000L,
            weightGrams = finalReading.weightGrams + extraIndex * 0.2
        )
    }

    companion object {
        private fun defaultReadingSequence(): List<ScaleReading> =
            listOf(
                ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
                ScaleReading(timestampMillis = 1_000L, weightGrams = 0.8),
                ScaleReading(timestampMillis = 2_000L, weightGrams = 2.0),
                ScaleReading(timestampMillis = 3_000L, weightGrams = 3.5),
                ScaleReading(timestampMillis = 4_000L, weightGrams = 5.2),
                ScaleReading(timestampMillis = 5_000L, weightGrams = 7.0),
                ScaleReading(timestampMillis = 6_000L, weightGrams = 8.8),
                ScaleReading(timestampMillis = 7_000L, weightGrams = 10.6),
                ScaleReading(timestampMillis = 8_000L, weightGrams = 12.5),
                ScaleReading(timestampMillis = 9_000L, weightGrams = 14.3),
                ScaleReading(timestampMillis = 10_000L, weightGrams = 16.1),
                ScaleReading(timestampMillis = 11_000L, weightGrams = 18.0),
                ScaleReading(timestampMillis = 12_000L, weightGrams = 19.8),
                ScaleReading(timestampMillis = 13_000L, weightGrams = 21.6),
                ScaleReading(timestampMillis = 14_000L, weightGrams = 23.3),
                ScaleReading(timestampMillis = 15_000L, weightGrams = 25.0),
                ScaleReading(timestampMillis = 16_000L, weightGrams = 26.6),
                ScaleReading(timestampMillis = 17_000L, weightGrams = 28.1),
                ScaleReading(timestampMillis = 18_000L, weightGrams = 29.5),
                ScaleReading(timestampMillis = 19_000L, weightGrams = 30.8),
                ScaleReading(timestampMillis = 20_000L, weightGrams = 32.0),
                ScaleReading(timestampMillis = 21_000L, weightGrams = 33.1),
                ScaleReading(timestampMillis = 22_000L, weightGrams = 34.1),
                ScaleReading(timestampMillis = 23_000L, weightGrams = 35.0),
                ScaleReading(timestampMillis = 24_000L, weightGrams = 35.8),
                ScaleReading(timestampMillis = 25_000L, weightGrams = 36.5),
                ScaleReading(timestampMillis = 26_000L, weightGrams = 37.0),
                ScaleReading(timestampMillis = 27_000L, weightGrams = 37.4),
                ScaleReading(timestampMillis = 28_000L, weightGrams = 37.7)
            )
    }
}
