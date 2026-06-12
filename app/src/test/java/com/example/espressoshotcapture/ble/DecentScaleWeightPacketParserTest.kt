package com.example.espressoshotcapture.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecentScaleWeightPacketParserTest {
    @Test
    fun parsesZeroWeightPacket() {
        val result = DecentScaleWeightPacketParser.parse(
            packet(0x03, 0xCE, 0x00, 0x00, 0x00, 0x00, 0xCD)
        )

        assertSuccessWeight(0.0, result)
    }

    @Test
    fun parsesPositiveWeightPacket() {
        val result = DecentScaleWeightPacketParser.parse(
            packet(0x03, 0xCE, 0x01, 0x6D, 0x00, 0x00, 0xA1)
        )

        assertSuccessWeight(36.5, result)
    }

    @Test
    fun parsesNegativeWeightPacket() {
        val result = DecentScaleWeightPacketParser.parse(
            packet(0x03, 0xCE, 0xFF, 0xF4, 0x00, 0x00, 0xC6)
        )

        assertSuccessWeight(-1.2, result)
    }

    @Test
    fun rejectsUnexpectedLength() {
        val result = DecentScaleWeightPacketParser.parse(packet(0x03, 0xCE))

        assertTrue(result is DecentScaleWeightPacketParseResult.Invalid)
    }

    @Test
    fun rejectsUnexpectedHeader() {
        val result = DecentScaleWeightPacketParser.parse(
            packet(0x04, 0xCE, 0x00, 0x00, 0x00, 0x00, 0xCA)
        )

        assertTrue(result is DecentScaleWeightPacketParseResult.Invalid)
    }

    @Test
    fun rejectsInvalidChecksum() {
        val result = DecentScaleWeightPacketParser.parse(
            packet(0x03, 0xCE, 0x00, 0x00, 0x00, 0x00, 0x00)
        )

        assertTrue(result is DecentScaleWeightPacketParseResult.Invalid)
    }

    @Test
    fun formatsRawPacketHex() {
        val hex = DecentScaleWeightPacketParser.toHex(
            packet(0x03, 0xCE, 0x01, 0x6D, 0x00, 0x00, 0xA1)
        )

        assertEquals("03 CE 01 6D 00 00 A1", hex)
    }

    private fun assertSuccessWeight(
        expectedWeightG: Double,
        result: DecentScaleWeightPacketParseResult
    ) {
        assertTrue(result is DecentScaleWeightPacketParseResult.Success)
        val success = result as DecentScaleWeightPacketParseResult.Success
        assertEquals(expectedWeightG, success.reading.weightGrams, 0.0001)
    }

    private fun packet(vararg bytes: Int): ByteArray =
        bytes.map { value -> value.toByte() }.toByteArray()
}
