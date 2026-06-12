package com.example.espressoshotcapture.ble

data class BleScaleScanCandidate(
    val name: String?,
    val address: String?,
    val advertisedServiceUuids: List<String>,
    val rssi: Int?,
    val matchesExpectedName: Boolean,
    val matchesExpectedService: Boolean
) {
    val isExpectedScale: Boolean
        get() = matchesExpectedName || matchesExpectedService

    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Unknown"

    val displayAddress: String
        get() = address ?: "Unknown address"

    val serviceUuidsLabel: String
        get() = if (advertisedServiceUuids.isEmpty()) {
            "Services: none advertised"
        } else {
            "Services: ${advertisedServiceUuids.joinToString()}"
        }

    val rssiLabel: String
        get() = rssi?.let { value -> "RSSI: $value dBm" } ?: "RSSI: unknown"

    val matchLabel: String
        get() = when {
            matchesExpectedName && matchesExpectedService -> "Matches Decent Scale name and service"
            matchesExpectedName -> "Matches Decent Scale name"
            matchesExpectedService -> "Matches Decent Scale service"
            else -> "No Decent Scale match"
        }
}

object BleScaleScanCandidateList {
    fun upsert(
        existing: List<BleScaleScanCandidate>,
        candidate: BleScaleScanCandidate
    ): List<BleScaleScanCandidate> {
        val deduped = existing
            .filterNot { existingCandidate -> existingCandidate.identityKey == candidate.identityKey }
            .plus(candidate)

        return sorted(deduped)
    }

    fun sorted(candidates: List<BleScaleScanCandidate>): List<BleScaleScanCandidate> =
        candidates.sortedWith(
            compareByDescending<BleScaleScanCandidate> { it.isExpectedScale }
                .thenByDescending { it.rssi ?: Int.MIN_VALUE }
                .thenBy { it.displayName.lowercase() }
                .thenBy { it.displayAddress }
        )

    private val BleScaleScanCandidate.identityKey: String
        get() = address ?: "${displayName}-${advertisedServiceUuids.joinToString()}"
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
