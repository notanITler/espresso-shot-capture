package com.example.espressoshotcapture.capture.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.withTimeoutOrNull

class FakeScaleClientTest {
    @Test
    fun initialStateIsDisconnected() = runTest {
        val client = FakeScaleClient()

        assertEquals(
            ScaleConnectionState.Disconnected,
            client.connectionState.first()
        )
    }

    @Test
    fun connectTransitionsToConnected() = runTest {
        val client = FakeScaleClient()

        client.connect()

        assertEquals(
            ScaleConnectionState.Connected,
            client.connectionState.first()
        )
    }

    @Test
    fun disconnectTransitionsToDisconnected() = runTest {
        val client = FakeScaleClient()

        client.connect()
        client.disconnect()

        assertEquals(
            ScaleConnectionState.Disconnected,
            client.connectionState.first()
        )
    }

    @Test
    fun readingsAreEmittedOnlyWhenConnected() = runTest {
        val client = FakeScaleClient(
            readingSequence = listOf(
                ScaleReading(timestampMillis = 1_000L, weightGrams = 18.0)
            )
        )

        assertFalse(client.emitNextReading())

        client.connect()

        assertTrue(client.emitNextReading())
        assertEquals(
            ScaleReading(timestampMillis = 1_000L, weightGrams = 18.0),
            client.readings.first()
        )
    }

    @Test
    fun readingsDoNotResetDuringOneConnectedSession() = runTest {
        val client = FakeScaleClient(
            readingSequence = listOf(
                ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
                ScaleReading(timestampMillis = 1_000L, weightGrams = 2.0)
            )
        )
        client.connect()

        client.emitNextReading()
        val first = client.readings.first()
        client.emitNextReading()
        val second = client.readings.first()
        client.emitNextReading()
        val third = client.readings.first()

        assertTrue(second.timestampMillis > first.timestampMillis)
        assertTrue(second.weightGrams > first.weightGrams)
        assertTrue(third.timestampMillis > second.timestampMillis)
        assertTrue(third.weightGrams > second.weightGrams)
    }

    @Test
    fun resetReadingsRestartsDeterministicSequence() = runTest {
        val client = FakeScaleClient(
            readingSequence = listOf(
                ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
                ScaleReading(timestampMillis = 1_000L, weightGrams = 2.0)
            )
        )
        client.connect()
        client.emitNextReading()
        client.emitNextReading()

        client.resetReadings()
        client.emitNextReading()

        assertEquals(
            ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
            client.readings.first()
        )
    }

    @Test
    fun resetReadingsClearsStaleReplayedReading() = runTest {
        val client = FakeScaleClient(
            readingSequence = listOf(
                ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
                ScaleReading(timestampMillis = 1_000L, weightGrams = 2.0)
            )
        )
        client.connect()
        client.emitNextReading()

        assertEquals(
            ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
            client.readings.first()
        )

        client.resetReadings()

        assertNull(withTimeoutOrNull(1L) { client.readings.first() })

        client.emitNextReading()

        assertEquals(
            ScaleReading(timestampMillis = 0L, weightGrams = 0.0),
            client.readings.first()
        )
    }
}
