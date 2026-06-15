package com.example.espressoshotcapture.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BleScaleScanViewModel(
    private val scanner: BleScaleScanner,
    private val gattClient: DecentScaleGattClient,
    private val onExpectedScaleSelected: (BleScaleScanCandidate) -> Unit = {}
) : ViewModel() {
    val uiState: StateFlow<BleScaleScanState> = scanner.scanState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BleScaleScanState()
    )
    val gattState: StateFlow<DecentScaleGattState> = gattClient.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DecentScaleGattState()
    )

    fun startScan() {
        scanner.startScan()
    }

    fun stopScan() {
        scanner.stopScan()
    }

    fun onPermissionsDenied() {
        scanner.markPermissionRequired()
    }

    fun connect(candidate: BleScaleScanCandidate) {
        if (!candidate.isExpectedScale) return
        onExpectedScaleSelected(candidate)
        scanner.stopScan()
        gattClient.connect(candidate)
    }

    fun sendTare() {
        gattClient.sendTare()
    }

    override fun onCleared() {
        scanner.stopScan()
        gattClient.disconnect()
        super.onCleared()
    }

    companion object {
        fun factory(
            scanner: BleScaleScanner,
            gattClient: DecentScaleGattClient,
            onExpectedScaleSelected: (BleScaleScanCandidate) -> Unit = {}
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(BleScaleScanViewModel::class.java)) {
                        return BleScaleScanViewModel(
                            scanner = scanner,
                            gattClient = gattClient,
                            onExpectedScaleSelected = onExpectedScaleSelected
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
