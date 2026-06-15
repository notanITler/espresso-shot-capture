package com.example.espressoshotcapture.ble

object DecentScaleCommandPackets {
    fun tare(): ByteArray =
        byteArrayOf(
            0x03,
            0x0F,
            0x00,
            0x00,
            0x00,
            0x01,
            0x0D
        )
}
