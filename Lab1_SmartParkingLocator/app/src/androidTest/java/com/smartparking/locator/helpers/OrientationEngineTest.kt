package com.smartparking.locator.helpers

import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrientationEngineTest {
    private val flat = floatArrayOf(0f, 0f, 9.81f)
    private val north = floatArrayOf(0f, 25f, -40f)

    @Test fun flatDeviceFacingNorthReturnsZeroAzimuth() {
        val result = OrientationEngine.calculate(flat, north, Surface.ROTATION_0)!!
        assertEquals(0f, result.azimuth, 0.1f)
        assertEquals(0f, result.pitch, 0.1f)
        assertEquals(0f, result.roll, 0.1f)
    }
    @Test fun eastAndWestAreNotMirrored() {
        assertEquals(90f, OrientationEngine.calculate(flat, floatArrayOf(-25f, 0f, -40f), 0)!!.azimuth, 0.1f)
        assertEquals(270f, OrientationEngine.calculate(flat, floatArrayOf(25f, 0f, -40f), 0)!!.azimuth, 0.1f)
    }
    @Test fun allDisplayRotationsRemapScreenTop() {
        for (rotation in 0..3)
            assertEquals(rotation * 90f, OrientationEngine.calculate(flat, north, rotation)!!.azimuth, 0.1f)
    }
    @Test fun invalidVectorsDoNotInventNorth() {
        assertNull(OrientationEngine.calculate(FloatArray(3), north, 0))
        assertNull(OrientationEngine.calculate(flat, FloatArray(3), 0))
        assertNull(OrientationEngine.calculate(flat, floatArrayOf(0f, 0f, 20f), 0))
        assertNull(OrientationEngine.calculate(floatArrayOf(Float.NaN, 0f, 9f), north, 0))
        assertNull(OrientationEngine.calculate(flat, floatArrayOf(0f, Float.POSITIVE_INFINITY, 0f), 0))
        assertNull(OrientationEngine.calculate(floatArrayOf(1f), north, 0))
        assertNull(OrientationEngine.calculate(flat, north, 7))
    }
    @Test fun nearVerticalTopHasNoHorizontalHeading() {
        assertNull(OrientationEngine.calculate(floatArrayOf(0f, 9.81f, 0f), floatArrayOf(0f, -40f, -25f), 0))
    }
    @Test fun tiltCompensationPreservesNorth() {
        // Обидва вектори повернуті на 30 градусів навколо осі X.
        val result = OrientationEngine.calculate(floatArrayOf(0f, 4.905f, 8.4957f),
            floatArrayOf(0f, 1.6506f, -47.141f), 0)!!
        assertEquals(0f, result.azimuth, 0.2f)
        assertEquals(-30f, result.pitch, 0.2f)
    }
}
