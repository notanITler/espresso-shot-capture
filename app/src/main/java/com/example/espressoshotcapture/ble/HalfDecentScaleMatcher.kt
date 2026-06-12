package com.example.espressoshotcapture.ble

object HalfDecentScaleMatcher {
    const val EXPECTED_DEVICE_NAME: String = "Decent Scale"
    const val SERVICE_UUID_SHORT: String = "fff0"
    const val SERVICE_UUID_FULL: String = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val READ_NOTIFY_CHARACTERISTIC_UUID_SHORT: String = "fff4"
    const val READ_NOTIFY_CHARACTERISTIC_UUID_FULL: String = "0000fff4-0000-1000-8000-00805f9b34fb"
    const val WRITE_CHARACTERISTIC_UUID_SHORT: String = "36f5"
    const val WRITE_CHARACTERISTIC_UUID_FULL: String = "000036f5-0000-1000-8000-00805f9b34fb"

    fun toCandidate(
        name: String?,
        address: String?,
        advertisedServiceUuids: List<String>,
        rssi: Int? = null
    ): BleScaleScanCandidate {
        val matchesExpectedName = name?.trim()
            ?.equals(EXPECTED_DEVICE_NAME, ignoreCase = true) == true
        val matchesExpectedService = advertisedServiceUuids.any(::matchesExpectedServiceUuid)

        return BleScaleScanCandidate(
            name = name,
            address = address,
            advertisedServiceUuids = advertisedServiceUuids,
            rssi = rssi,
            matchesExpectedName = matchesExpectedName,
            matchesExpectedService = matchesExpectedService
        )
    }

    private fun matchesExpectedServiceUuid(uuid: String): Boolean {
        val normalized = uuid.trim().lowercase()
        return normalized == SERVICE_UUID_SHORT || normalized == SERVICE_UUID_FULL
    }
}
