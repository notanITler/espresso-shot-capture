package com.example.espressoshotcapture.capture.engine

import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.ShotSource
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
}
