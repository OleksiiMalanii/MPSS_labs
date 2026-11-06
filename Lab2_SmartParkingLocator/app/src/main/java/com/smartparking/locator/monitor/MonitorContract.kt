package com.smartparking.locator.monitor

import com.smartparking.locator.helpers.Orientation

data class Vector3(val x: Float, val y: Float, val z: Float) {
    fun array() = floatArrayOf(x, y, z)
}

// State — усе, що показуємо на екрані.
data class MonitorState(
    val acceleration: Vector3? = null,
    val magnetic: Vector3? = null,
    val orientation: Orientation? = null,
    val tiltWarning: Boolean = false,
    val listening: Boolean = false,
    val error: String? = null,
    val magnetometerUnreliable: Boolean = false,
)

// Action — дії, які Activity передає у ViewModel.
sealed interface MonitorAction {
    data object Start : MonitorAction
    data object Stop : MonitorAction
}
