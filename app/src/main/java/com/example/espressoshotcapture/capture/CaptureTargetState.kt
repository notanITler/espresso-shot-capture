package com.example.espressoshotcapture.capture

import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.ShotSource

data class CaptureTargetState(
    val doseGrams: Double,
    val targetYieldGrams: Double
) {
    val ratio: Double?
        get() = if (isValid) {
            targetYieldGrams / doseGrams
        } else {
            null
        }

    val isValid: Boolean
        get() = doseGrams > 0.0 && targetYieldGrams > 0.0

    fun toCaptureTargetOrNull(): CaptureTarget? {
        val targetRatio = ratio ?: return null
        return CaptureTarget(
            source = ShotSource.QUICK_SHOT,
            recipeId = null,
            beanId = null,
            doseG = doseGrams,
            targetRatio = targetRatio,
            targetYieldG = targetYieldGrams,
            targetTimeS = null
        )
    }
}
