package com.example.espressoshotcapture.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BleScaleScanViewModel(
    private val scanner: BleScaleScanner
) : ViewModel() {
    val uiState: StateFlow<BleScaleScanState> = scanner.scanState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BleScaleScanState()
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

    override fun onCleared() {
        scanner.stopScan()
        super.onCleared()
    }

    companion object {
        fun factory(scanner: BleScaleScanner): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(BleScaleScanViewModel::class.java)) {
                        return BleScaleScanViewModel(scanner) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
