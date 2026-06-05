package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.CapturedSample
import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.ShotDraft
import com.example.espressoshotcapture.capture.domain.ShotResult
import com.example.espressoshotcapture.capture.domain.ShotStatus
import com.example.espressoshotcapture.capture.domain.ShotTiming
import com.example.espressoshotcapture.capture.domain.StartMode
import com.example.espressoshotcapture.capture.domain.StopMode
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

    var targetReachedAtMs: Long? = null
        private set

    var targetReachedWeightG: Double? = null
        private set

    val recordedSamples: List<CapturedSample>
        get() = capturedSamples.toList()

    var completedShotDraft: ShotDraft? = null
        private set

    private var isScaleConnected: Boolean = false
    private val armedSamples = mutableListOf<WeightSample>()
    private val capturedSamples = mutableListOf<CapturedSample>()
    private var previousRecordedSample: WeightSample? = null

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
        armedSamples.clear()
        capturedSamples.clear()
        previousRecordedSample = null
        recordingStartedAtMs = null
        targetReachedAtMs = null
        targetReachedWeightG = null
        completedShotDraft = null
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
        when (state) {
            ShotCaptureState.ARMED -> handleArmedSample(sample)
            ShotCaptureState.RECORDING -> handleRecordingSample(sample)
            else -> return
        }
    }

    fun reset() {
        activeTarget = null
        armedSamples.clear()
        capturedSamples.clear()
        previousRecordedSample = null
        recordingStartedAtMs = null
        targetReachedAtMs = null
        targetReachedWeightG = null
        completedShotDraft = null
        state = if (isScaleConnected) {
            ShotCaptureState.CONNECTED_IDLE
        } else {
            ShotCaptureState.DISCONNECTED
        }
    }

    private fun handleArmedSample(sample: WeightSample) {
        armedSamples += sample
        trimRecentSamples()

        if (StartPolicy.shouldStart(state = state, recentSamples = armedSamples, config = config)) {
            recordingStartedAtMs = sample.timestampMs
            state = ShotCaptureState.RECORDING
        }
    }

    private fun handleRecordingSample(sample: WeightSample) {
        recordSample(sample)
        evaluateStopPolicy(sample)
    }

    private fun recordSample(sample: WeightSample) {
        val recordingStartTimestampMs = recordingStartedAtMs ?: sample.timestampMs
        capturedSamples += SamplingPolicy.toCapturedSample(
            index = capturedSamples.size,
            recordingStartTimestampMs = recordingStartTimestampMs,
            currentSample = sample,
            previousSample = previousRecordedSample,
            config = config
        )
        previousRecordedSample = sample
    }

    private fun evaluateStopPolicy(sample: WeightSample) {
        val target = activeTarget ?: return

        when (
            StopPolicy.evaluate(
                currentWeightG = sample.weightG,
                targetYieldG = target.targetYieldG,
                targetReachedAtMs = targetReachedAtMs,
                currentTimestampMs = sample.timestampMs,
                config = config
            )
        ) {
            StopDecision.CONTINUE -> Unit
            StopDecision.TARGET_REACHED -> {
                if (targetReachedAtMs == null) {
                    targetReachedAtMs = sample.timestampMs
                    targetReachedWeightG = sample.weightG
                }
            }
            StopDecision.COMPLETE -> {
                completedShotDraft = createCompletedShotDraft(target)
                state = ShotCaptureState.SAVED
            }
        }
    }

    private fun createCompletedShotDraft(target: CaptureTarget): ShotDraft {
        val recordingStartTimestampMs = recordingStartedAtMs ?: 0L
        val samples = recordedSamples
        val actualYieldG = samples.lastOrNull()?.weightGRaw

        return ShotDraft(
            id = "shot-$recordingStartTimestampMs",
            createdAtEpochMs = recordingStartTimestampMs,
            target = target,
            timing = ShotTiming(
                startMode = StartMode.AUTO_WEIGHT,
                stopMode = StopMode.TARGET_YIELD,
                brewTimeMs = null,
                flowTimeMs = samples.lastOrNull()?.tMs ?: 0L,
                targetReachedAtMs = targetReachedAtMs?.minus(recordingStartTimestampMs),
                firstWeightDelayMs = null,
                postTargetRecordingMs = config.postTargetRecordingMs
            ),
            result = ShotResult(
                actualYieldG = actualYieldG,
                postTargetDriftG = actualYieldG?.minus(target.targetYieldG),
                averageFlowGPerS = null,
                maxFlowGPerS = null,
                sampleCount = samples.size
            ),
            samples = samples,
            status = ShotStatus.COMPLETED,
            notes = null
        )
    }

    private fun trimRecentSamples() {
        val maxSampleCount = config.startConfirmationSamples
        if (maxSampleCount <= 0) {
            armedSamples.clear()
            return
        }

        while (armedSamples.size > maxSampleCount) {
            armedSamples.removeAt(0)
        }
    }

    private fun CaptureTarget.isValid(): Boolean =
        doseG > 0.0 && targetYieldG > 0.0 && targetRatio > 0.0
}
