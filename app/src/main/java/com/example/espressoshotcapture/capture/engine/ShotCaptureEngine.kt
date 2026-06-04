package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.CaptureTarget

class ShotCaptureEngine(
    @Suppress("unused") private val config: ShotCaptureConfig = ShotCaptureConfig()
) {
    var state: ShotCaptureState = ShotCaptureState.DISCONNECTED
        private set

    var activeTarget: CaptureTarget? = null
        private set

    private var isScaleConnected: Boolean = false

    fun onScaleConnected() {
        isScaleConnected = true
        if (state == ShotCaptureState.DISCONNECTED) {
            state = ShotCaptureState.CONNECTED_IDLE
        }
    }

    fun onScaleDisconnected() {
        isScaleConnected = false
        state = ShotCaptureState.DISCONNECTED
        activeTarget = null
    }

    fun onTareConfirmed() {
        if (state == ShotCaptureState.CONNECTED_IDLE) {
            state = ShotCaptureState.TARED
        }
    }

    fun arm(target: CaptureTarget): Boolean {
        if (state != ShotCaptureState.TARED) {
            return false
        }

        if (!target.isValid()) {
            state = ShotCaptureState.ERROR
            activeTarget = null
            return false
        }

        activeTarget = target
        state = ShotCaptureState.ARMED
        return true
    }

    fun reset() {
        activeTarget = null
        state = if (isScaleConnected) {
            ShotCaptureState.CONNECTED_IDLE
        } else {
            ShotCaptureState.DISCONNECTED
        }
    }

    private fun CaptureTarget.isValid(): Boolean =
        doseG > 0.0 && targetYieldG > 0.0 && targetRatio > 0.0
}
