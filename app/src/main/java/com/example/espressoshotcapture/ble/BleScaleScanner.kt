package com.example.espressoshotcapture.ble

import kotlinx.coroutines.flow.StateFlow

interface BleScaleScanner {
    val scanState: StateFlow<BleScaleScanState>

    fun startScan()
    fun stopScan()
    fun markPermissionRequired()
}
