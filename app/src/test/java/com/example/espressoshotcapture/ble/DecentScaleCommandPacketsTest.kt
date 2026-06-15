package com.example.espressoshotcapture.ble

import org.junit.Assert.assertEquals
import org.junit.Test

class DecentScaleCommandPacketsTest {
    @Test
    fun tareUsesOpenScaleDocumentedPacket() {
        assertEquals(
            "03 0F 00 00 00 01 0D",
            DecentScaleWeightPacketParser.toHex(DecentScaleCommandPackets.tare())
        )
    }
}
