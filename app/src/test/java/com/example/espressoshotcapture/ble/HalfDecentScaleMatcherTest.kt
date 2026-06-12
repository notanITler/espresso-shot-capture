package com.example.espressoshotcapture.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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

    @Test
    fun sortsExpectedMatchesBeforeStrongerUnrelatedDevices() {
        val unrelatedStrongSignal = HalfDecentScaleMatcher.toCandidate(
            name = "Kitchen Speaker",
            address = "AA:BB:CC:DD:EE:01",
            advertisedServiceUuids = emptyList(),
            rssi = -30
        )
        val expectedWeakSignal = HalfDecentScaleMatcher.toCandidate(
            name = "Decent Scale",
            address = "AA:BB:CC:DD:EE:02",
            advertisedServiceUuids = emptyList(),
            rssi = -80
        )

        val sorted = BleScaleScanCandidateList.sorted(
            listOf(unrelatedStrongSignal, expectedWeakSignal)
        )

        assertEquals(expectedWeakSignal, sorted.first())
    }

    @Test
    fun sortsSameMatchGroupByStrongerRssiFirst() {
        val weakSignal = HalfDecentScaleMatcher.toCandidate(
            name = "Device A",
            address = "AA:BB:CC:DD:EE:01",
            advertisedServiceUuids = emptyList(),
            rssi = -80
        )
        val strongSignal = HalfDecentScaleMatcher.toCandidate(
            name = "Device B",
            address = "AA:BB:CC:DD:EE:02",
            advertisedServiceUuids = emptyList(),
            rssi = -40
        )

        val sorted = BleScaleScanCandidateList.sorted(listOf(weakSignal, strongSignal))

        assertEquals(strongSignal, sorted.first())
    }

    @Test
    fun upsertReplacesExistingCandidateWithSameAddress() {
        val original = HalfDecentScaleMatcher.toCandidate(
            name = "Unknown",
            address = "AA:BB:CC:DD:EE:01",
            advertisedServiceUuids = emptyList(),
            rssi = -80
        )
        val updated = HalfDecentScaleMatcher.toCandidate(
            name = "Decent Scale",
            address = "AA:BB:CC:DD:EE:01",
            advertisedServiceUuids = listOf("fff0"),
            rssi = -45
        )

        val candidates = BleScaleScanCandidateList.upsert(
            existing = listOf(original),
            candidate = updated
        )

        assertEquals(1, candidates.size)
        assertEquals(updated, candidates.single())
    }
}
