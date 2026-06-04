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

    private fun connectedIdleEngine(): ShotCaptureEngine =
        ShotCaptureEngine().also { engine ->
            engine.onScaleConnected()
        }

    private fun taredEngine(): ShotCaptureEngine =
        connectedIdleEngine().also { engine ->
            engine.onTareConfirmed()
        }

    private fun armedEngine(): ShotCaptureEngine =
        taredEngine().also { engine ->
            engine.arm(validTarget())
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
