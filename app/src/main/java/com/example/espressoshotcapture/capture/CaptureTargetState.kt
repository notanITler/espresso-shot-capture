package com.example.espressoshotcapture.capture

import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.ShotSource
import kotlin.math.roundToInt

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
        get() = doseGrams.isPositiveFinite() && targetYieldGrams.isPositiveFinite()

    val doseInputValue: String
        get() = doseGrams.toInputValue()

    val targetYieldInputValue: String
        get() = targetYieldGrams.toInputValue()

    val ratioLabel: String
        get() = ratio?.let { targetRatio ->
            "Ratio: 1:${targetRatio.toRatioValue()}"
        } ?: "Ratio: --"

    val validationMessage: String?
        get() = if (isValid) {
            null
        } else {
            "Enter a positive dose and target yield."
        }

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

    private fun Double.toInputValue(): String =
        if (!isNaN() && !isInfinite()) {
            toString()
        } else {
            ""
        }

    private fun Double.isPositiveFinite(): Boolean =
        this > 0.0 && !isNaN() && !isInfinite()

    private fun Double.toRatioValue(): String {
        val rounded = roundToInt()
        return if (this == rounded.toDouble()) {
            rounded.toString()
        } else {
            ((this * 10.0).roundToInt() / 10.0).toString()
        }
    }
}
