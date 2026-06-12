package com.example.espressoshotcapture.ble

data class DecentScaleWeightReading(
    val weightGrams: Double
)

sealed class DecentScaleWeightPacketParseResult {
    data class Success(val reading: DecentScaleWeightReading) : DecentScaleWeightPacketParseResult()
    data class Invalid(val reason: String) : DecentScaleWeightPacketParseResult()
}

object DecentScaleWeightPacketParser {
    private const val WEIGHT_PACKET_LENGTH = 7
    private const val MODEL_BYTE = 0x03
    private const val WEIGHT_MESSAGE_TYPE = 0xCE

    fun parse(packet: ByteArray): DecentScaleWeightPacketParseResult {
        if (packet.size != WEIGHT_PACKET_LENGTH) {
            return DecentScaleWeightPacketParseResult.Invalid(
                "Expected 7-byte weight packet, got ${packet.size}"
            )
        }

        if (packet[0].toUnsignedInt() != MODEL_BYTE) {
            return DecentScaleWeightPacketParseResult.Invalid("Unexpected model byte")
        }

        if (packet[1].toUnsignedInt() != WEIGHT_MESSAGE_TYPE) {
            return DecentScaleWeightPacketParseResult.Invalid("Unexpected message type")
        }

        val expectedChecksum = calculateXor(packet, endExclusive = packet.lastIndex)
        val actualChecksum = packet.last().toUnsignedInt()
        if (actualChecksum != expectedChecksum) {
            return DecentScaleWeightPacketParseResult.Invalid("Invalid checksum")
        }

        val rawTenthsOfGram = (packet[2].toUnsignedInt() shl 8) or packet[3].toUnsignedInt()
        val signedTenthsOfGram = rawTenthsOfGram.toShort().toInt()
        return DecentScaleWeightPacketParseResult.Success(
            DecentScaleWeightReading(weightGrams = signedTenthsOfGram / 10.0)
        )
    }

    fun toHex(packet: ByteArray): String =
        packet.joinToString(separator = " ") { byte ->
            byte.toUnsignedInt().toString(radix = 16).padStart(2, '0').uppercase()
        }

    private fun calculateXor(packet: ByteArray, endExclusive: Int): Int {
        var checksum = 0
        for (index in 0 until endExclusive) {
            checksum = checksum xor packet[index].toUnsignedInt()
        }
        return checksum
    }

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
}
