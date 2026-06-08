package com.example.espressoshotcapture.capture

import com.example.espressoshotcapture.capture.domain.CaptureTarget
import com.example.espressoshotcapture.capture.domain.ShotSource
import kotlin.math.roundToInt

object MvpShotTarget {
    const val DOSE_G: Double = 18.0
    const val TARGET_YIELD_G: Double = 36.0
    const val TARGET_RATIO: Double = 2.0

    const val DOSE_LABEL: String = "Dose: 18.0 g"
    const val TARGET_YIELD_LABEL: String = "Target yield: 36.0 g"
    const val RATIO_LABEL: String = "Ratio: 1:2"
    const val EMPTY_PROGRESS_LABEL: String = "Progress: 0.0 / 36.0 g"
    const val TARGET_NOT_REACHED_LABEL: String = "Target not reached"
    const val TARGET_REACHED_LABEL: String = "Target reached"

    fun progressLabel(currentWeightG: Double): String =
        "Progress: ${currentWeightG.toOneDecimal()} / ${TARGET_YIELD_G.toOneDecimal()} g"

    fun targetReachedLabel(currentWeightG: Double): String =
        if (currentWeightG >= TARGET_YIELD_G) {
            TARGET_REACHED_LABEL
        } else {
            TARGET_NOT_REACHED_LABEL
        }

    fun toCaptureTarget(): CaptureTarget =
        CaptureTarget(
            source = ShotSource.QUICK_SHOT,
            recipeId = null,
            beanId = null,
            doseG = DOSE_G,
            targetRatio = TARGET_RATIO,
            targetYieldG = TARGET_YIELD_G,
            targetTimeS = null
        )

    private fun Double.toOneDecimal(): String =
        ((this * 10.0).roundToInt() / 10.0).toString()
}
