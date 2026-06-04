package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.WeightSample

class ShotCaptureEngine(
    private val config: ShotCaptureConfig = ShotCaptureConfig()
) {
    var state: ShotCaptureState = ShotCaptureState.DISCONNECTED
        private set

    var activeTarget: CaptureTarget? = null
        private set

    var recordingStartedAtMs: Long? = null
        private set

    private var isScaleConnected: Boolean = false
    private val recentSamples = mutableListOf<WeightSample>()

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
        recentSamples.clear()
        recordingStartedAtMs = null
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

    fun onWeightSample(sample: WeightSample) {
        if (state != ShotCaptureState.ARMED) {
            return
        }

        recentSamples += sample
        trimRecentSamples()

        if (StartPolicy.shouldStart(state = state, recentSamples = recentSamples, config = config)) {
            recordingStartedAtMs = sample.timestampMs
            state = ShotCaptureState.RECORDING
        }
    }

    fun reset() {
        activeTarget = null
        recentSamples.clear()
        recordingStartedAtMs = null
        state = if (isScaleConnected) {
            ShotCaptureState.CONNECTED_IDLE
        } else {
            ShotCaptureState.DISCONNECTED
        }
    }

    private fun trimRecentSamples() {
        val maxSampleCount = config.startConfirmationSamples
        if (maxSampleCount <= 0) {
            recentSamples.clear()
            return
        }

        while (recentSamples.size > maxSampleCount) {
            recentSamples.removeAt(0)
        }
    }

    private fun CaptureTarget.isValid(): Boolean =
        doseG > 0.0 && targetYieldG > 0.0 && targetRatio > 0.0
}
