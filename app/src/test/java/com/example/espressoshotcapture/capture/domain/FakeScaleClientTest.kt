package com.example.espressoshotcapture.capture.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
