package com.example.espressoshotcapture.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HalfDecentScaleMatcherTest {
    @Test
    fun matchesExpectedAdvertisedName() {
        val candidate = HalfDecentScaleMatcher.toCandidate(
            name = "Decent Scale",
            address = "AA:BB:CC:DD:EE:FF",
            advertisedServiceUuids = emptyList()
        )

        assertTrue(candidate.matchesExpectedName)
        assertTrue(candidate.isExpectedScale)
    }

    @Test
    fun matchesExpectedShortServiceUuid() {
        val candidate = HalfDecentScaleMatcher.toCandidate(
            name = "Unknown",
            address = "AA:BB:CC:DD:EE:FF",
            advertisedServiceUuids = listOf("fff0")
        )

        assertTrue(candidate.matchesExpectedService)
        assertTrue(candidate.isExpectedScale)
    }

    @Test
    fun matchesExpectedFullServiceUuid() {
        val candidate = HalfDecentScaleMatcher.toCandidate(
            name = "Unknown",
            address = "AA:BB:CC:DD:EE:FF",
            advertisedServiceUuids = listOf("0000fff0-0000-1000-8000-00805f9b34fb")
        )

        assertTrue(candidate.matchesExpectedService)
        assertTrue(candidate.isExpectedScale)
    }

    @Test
    fun doesNotMatchUnrelatedDevice() {
        val candidate = HalfDecentScaleMatcher.toCandidate(
            name = "Kitchen Speaker",
            address = "AA:BB:CC:DD:EE:FF",
            advertisedServiceUuids = listOf("0000180f-0000-1000-8000-00805f9b34fb")
        )

        assertFalse(candidate.matchesExpectedName)
        assertFalse(candidate.matchesExpectedService)
        assertFalse(candidate.isExpectedScale)
    }
}
