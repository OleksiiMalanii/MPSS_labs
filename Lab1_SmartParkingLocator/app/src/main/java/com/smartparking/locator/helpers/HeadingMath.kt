package com.smartparking.locator.helpers

object HeadingMath {
    fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    fun direction(degrees: Float): String {
        require(degrees.isFinite())
        val names = listOf("Пн", "Пн-Сх", "Сх", "Пд-Сх", "Пд", "Пд-Зх", "Зх", "Пн-Зх")
        return names[((normalizeDegrees(degrees) + 22.5f) / 45f).toInt() % 8]
    }

    // Перехід 359 -> 1 становить +2, а не -358 градусів.
    fun signedDifference(from: Float, to: Float): Float = normalizeDegrees(to - from + 180f) - 180f

    const val TILT_THRESHOLD = 15f
    fun excessiveTilt(pitch: Float, roll: Float): Boolean =
        kotlin.math.abs(pitch) > TILT_THRESHOLD || kotlin.math.abs(roll) > TILT_THRESHOLD
}
