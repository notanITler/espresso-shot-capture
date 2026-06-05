package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.ShotSource
import com.example.espressoshotcapture.capture.domain.WeightSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotCaptureEngineTest {
    @Test
    fun initialStateIsDisconnected() {
        val engine = ShotCaptureEngine()

        assertEquals(ShotCaptureState.DISCONNECTED, engine.state)
        assertNull(engine.activeTarget)
    }

    @Test
    fun scaleConnectedMovesToConnectedIdle() {
        val engine = ShotCaptureEngine()

        engine.onScaleConnected()

        assertEquals(ShotCaptureState.CONNECTED_IDLE, engine.state)
    }

    @Test
    fun scaleDisconnectedFromAnyStateMovesToDisconnected() {
        listOf(
            connectedIdleEngine(),
            taredEngine(),
            armedEngine(),
            errorEngine()
        ).forEach { engine ->
            engine.onScaleDisconnected()

            assertEquals(ShotCaptureState.DISCONNECTED, engine.state)
            assertNull(engine.activeTarget)
        }
    }

    @Test
    fun tareConfirmedFromConnectedIdleMovesToTared() {
        val engine = connectedIdleEngine()

        engine.onTareConfirmed()

        assertEquals(ShotCaptureState.TARED, engine.state)
    }

    @Test
    fun armWithValidTargetFromTaredMovesToArmed() {
        val engine = taredEngine()

        assertTrue(engine.arm(validTarget()))

        assertEquals(ShotCaptureState.ARMED, engine.state)
    }

    @Test
    fun armStoresActiveTarget() {
        val engine = taredEngine()
        val target = validTarget()

        engine.arm(target)

        assertSame(target, engine.activeTarget)
    }

    @Test
    fun armWithInvalidTargetFails() {
        val engine = taredEngine()

        assertFalse(engine.arm(validTarget(doseG = 0.0)))

        assertEquals(ShotCaptureState.ERROR, engine.state)
        assertNull(engine.activeTarget)
    }

    @Test
    fun resetWhileConnectedMovesToConnectedIdle() {
        val engine = armedEngine()

        engine.reset()

        assertEquals(ShotCaptureState.CONNECTED_IDLE, engine.state)
        assertNull(engine.activeTarget)
    }

    @Test
    fun resetWhileDisconnectedMovesToDisconnected() {
        val engine = armedEngine()
        engine.onScaleDisconnected()

        engine.reset()

        assertEquals(ShotCaptureState.DISCONNECTED, engine.state)
        assertNull(engine.activeTarget)
    }

    @Test
    fun armedStateStartsRecordingOnStableWeightIncrease() {
        val engine = armedEngine()

        engine.addSamples(0.1, 0.3, 0.5)

        assertEquals(ShotCaptureState.RECORDING, engine.state)
    }

    @Test
    fun recordingStartedTimestampIsStored() {
        val engine = armedEngine()

        engine.addSamples(0.1, 0.3, 0.5)

        assertEquals(2000L, engine.recordingStartedAtMs)
    }

    @Test
    fun nonArmedStatesDoNotStartRecording() {
        listOf(
            ShotCaptureEngine(),
            connectedIdleEngine(),
            taredEngine()
        ).forEach { engine ->
            engine.addSamples(0.1, 0.3, 0.5)

            assertFalse(engine.state == ShotCaptureState.RECORDING)
            assertNull(engine.recordingStartedAtMs)
        }
    }

    @Test
    fun resetClearsRecordingStartTimestamp() {
        val engine = armedEngine()
        engine.addSamples(0.1, 0.3, 0.5)

        engine.reset()

        assertNull(engine.recordingStartedAtMs)
    }

    @Test
    fun resetClearsSampleBuffer() {
        val engine = armedEngine()
        engine.addSamples(0.1, 0.3)

        engine.reset()
        engine.onTareConfirmed()
        assertTrue(engine.arm(validTarget()))
        engine.onWeightSample(sample(timestampMs = 2000, weightG = 0.5))

        assertEquals(ShotCaptureState.ARMED, engine.state)
        assertNull(engine.recordingStartedAtMs)
    }

    @Test
    fun recordingDoesNotStartOnNoise() {
        val engine = armedEngine()

        engine.addSamples(0.1, 0.8, 0.5)

        assertEquals(ShotCaptureState.ARMED, engine.state)
        assertNull(engine.recordingStartedAtMs)
    }

    @Test
    fun recordingDoesNotStartOnWeightTouch() {
        val engine = armedEngine()

        engine.addSamples(0.0, 0.4, 0.0)

        assertEquals(ShotCaptureState.ARMED, engine.state)
        assertNull(engine.recordingStartedAtMs)
    }

    @Test
    fun recordingStoresCapturedSamples() {
        val engine = recordingEngine()

        engine.onWeightSample(sample(timestampMs = 2500, weightG = 0.8))
        engine.onWeightSample(sample(timestampMs = 3000, weightG = 1.2))

        assertEquals(2, engine.recordedSamples.size)
    }

    @Test
    fun sampleOrderIsPreserved() {
        val engine = recordingEngine()

        engine.onWeightSample(sample(timestampMs = 2500, weightG = 0.8))
        engine.onWeightSample(sample(timestampMs = 3000, weightG = 1.2))
        engine.onWeightSample(sample(timestampMs = 3500, weightG = 1.6))

        assertEquals(
            listOf(0.8, 1.2, 1.6),
            engine.recordedSamples.map { it.weightGRaw }
        )
    }

    @Test
    fun resetClearsRecordedSamples() {
        val engine = recordingEngine()
        engine.onWeightSample(sample(timestampMs = 2500, weightG = 0.8))

        engine.reset()

        assertTrue(engine.recordedSamples.isEmpty())
    }

    @Test
    fun samplesUseRelativeRecordingTime() {
        val engine = recordingEngine()

        engine.onWeightSample(sample(timestampMs = 2750, weightG = 0.8))

        assertEquals(750L, engine.recordedSamples.single().tMs)
    }

    @Test
    fun outlierDetectionUsesSamplingPolicy() {
        val engine = recordingEngine(
            config = ShotCaptureConfig(outlierJumpThresholdG = 1.0)
        )

        engine.onWeightSample(sample(timestampMs = 2500, weightG = 1.0))
        engine.onWeightSample(sample(timestampMs = 2600, weightG = 2.1))

        assertEquals(
            listOf(false, true),
            engine.recordedSamples.map { it.isOutlier }
        )
    }

    @Test
    fun armedSamplesAreNotAddedToRecordedSamples() {
        val engine = armedEngine()

        engine.addSamples(0.1, 0.3, 0.5)

        assertEquals(ShotCaptureState.RECORDING, engine.state)
        assertTrue(engine.recordedSamples.isEmpty())
    }

    @Test
    fun recordsTargetReachedTimestamp() {
        val engine = recordingEngine(target = validTarget(targetYieldG = 1.0))

        engine.onWeightSample(sample(timestampMs = 2500, weightG = 1.1))

        assertEquals(2500L, engine.targetReachedAtMs)
    }

    @Test
    fun recordsTargetReachedWeight() {
        val engine = recordingEngine(target = validTarget(targetYieldG = 1.0))

        engine.onWeightSample(sample(timestampMs = 2500, weightG = 1.1))

        assertEquals(1.1, engine.targetReachedWeightG ?: error("Expected target reached weight"), 0.0)
    }

    @Test
    fun targetReachedOnlyRecordedOnce() {
        val engine = recordingEngine(target = validTarget(targetYieldG = 1.0))

        engine.onWeightSample(sample(timestampMs = 2500, weightG = 1.1))
        engine.onWeightSample(sample(timestampMs = 2600, weightG = 1.2))

        assertEquals(2500L, engine.targetReachedAtMs)
        assertEquals(1.1, engine.targetReachedWeightG ?: error("Expected target reached weight"), 0.0)
    }

    @Test
    fun continuesCapturingSamplesAfterTargetReached() {
        val engine = recordingEngine(target = validTarget(targetYieldG = 1.0))

        engine.onWeightSample(sample(timestampMs = 2500, weightG = 1.1))
        engine.onWeightSample(sample(timestampMs = 3000, weightG = 1.3))

        assertEquals(ShotCaptureState.RECORDING, engine.state)
        assertEquals(
            listOf(1.1, 1.3),
            engine.recordedSamples.map { it.weightGRaw }
        )
    }

    @Test
    fun savedStateReachedAfterPostTargetWindow() {
        val engine = recordingEngine(target = validTarget(targetYieldG = 1.0))

        engine.onWeightSample(sample(timestampMs = 2500, weightG = 1.1))
        engine.onWeightSample(sample(timestampMs = 4000, weightG = 1.4))

        assertEquals(ShotCaptureState.SAVED, engine.state)
        assertEquals(
            listOf(1.1, 1.4),
            engine.recordedSamples.map { it.weightGRaw }
        )
    }

    @Test
    fun resetClearsTargetReachedInformation() {
        val engine = recordingEngine(target = validTarget(targetYieldG = 1.0))
        engine.onWeightSample(sample(timestampMs = 2500, weightG = 1.1))

        engine.reset()

        assertNull(engine.targetReachedAtMs)
        assertNull(engine.targetReachedWeightG)
    }

    private fun connectedIdleEngine(config: ShotCaptureConfig = ShotCaptureConfig()): ShotCaptureEngine =
        ShotCaptureEngine(config = config).also { engine ->
            engine.onScaleConnected()
        }

    private fun taredEngine(config: ShotCaptureConfig = ShotCaptureConfig()): ShotCaptureEngine =
        connectedIdleEngine(config = config).also { engine ->
            engine.onTareConfirmed()
        }

    private fun armedEngine(
        config: ShotCaptureConfig = ShotCaptureConfig(),
        target: CaptureTarget = validTarget()
    ): ShotCaptureEngine =
        taredEngine(config = config).also { engine ->
            engine.arm(target)
        }

    private fun recordingEngine(
        config: ShotCaptureConfig = ShotCaptureConfig(),
        target: CaptureTarget = validTarget()
    ): ShotCaptureEngine =
        armedEngine(config = config, target = target).also { engine ->
            engine.addSamples(0.1, 0.3, 0.5)
        }

    private fun errorEngine(): ShotCaptureEngine =
        taredEngine().also { engine ->
            engine.arm(validTarget(doseG = 0.0))
        }

    private fun validTarget(
        doseG: Double = 18.0,
        targetRatio: Double = 2.0,
        targetYieldG: Double = 36.0
    ): CaptureTarget =
        CaptureTarget(
            source = ShotSource.QUICK_SHOT,
            recipeId = null,
            beanId = null,
            doseG = doseG,
            targetRatio = targetRatio,
            targetYieldG = targetYieldG,
            targetTimeS = null
        )

    private fun ShotCaptureEngine.addSamples(vararg weightsG: Double, intervalMs: Long = 1000) {
        weightsG.forEachIndexed { index, weightG ->
            onWeightSample(
                sample(
                    timestampMs = index * intervalMs,
                    weightG = weightG
                )
            )
        }
    }

    private fun sample(timestampMs: Long, weightG: Double): WeightSample =
        WeightSample(
            timestampMs = timestampMs,
            weightG = weightG
        )
}
