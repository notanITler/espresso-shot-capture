package com.example.espressoshotcapture.ble

data class BleScaleScanCandidate(
    val name: String?,
    val address: String?,
    val advertisedServiceUuids: List<String>,
    val matchesExpectedName: Boolean,
    val matchesExpectedService: Boolean
) {
    val isExpectedScale: Boolean
        get() = matchesExpectedName || matchesExpectedService

    val displayName: String
        get() = name ?: "Unnamed device"

    val displayAddress: String
        get() = address ?: "Unknown address"

    val matchLabel: String
        get() = when {
            matchesExpectedName && matchesExpectedService -> "Matches Decent Scale name and service"
            matchesExpectedName -> "Matches Decent Scale name"
            matchesExpectedService -> "Matches Decent Scale service"
            else -> "No Decent Scale match"
        }
}

enum class BleScaleScanStatus {
    IDLE,
    SCANNING,
    PERMISSION_REQUIRED,
    BLUETOOTH_UNAVAILABLE,
    ERROR
}

data class BleScaleScanState(
    val status: BleScaleScanStatus = BleScaleScanStatus.IDLE,
    val candidates: List<BleScaleScanCandidate> = emptyList(),
    val errorMessage: String? = null
) {
    val statusLabel: String
        get() = when (status) {
            BleScaleScanStatus.IDLE -> "BLE scan: idle"
            BleScaleScanStatus.SCANNING -> "BLE scan: scanning"
            BleScaleScanStatus.PERMISSION_REQUIRED -> "BLE scan: permission required"
            BleScaleScanStatus.BLUETOOTH_UNAVAILABLE -> "BLE scan: Bluetooth unavailable"
            BleScaleScanStatus.ERROR -> "BLE scan: error"
        }
}
