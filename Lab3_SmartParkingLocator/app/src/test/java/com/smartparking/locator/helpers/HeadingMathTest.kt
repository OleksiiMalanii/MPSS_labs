package com.smartparking.locator.helpers

import org.junit.Assert.*
import org.junit.Test

class HeadingMathTest {
    @Test fun normalizationHandlesNegativeAndMultipleTurns() {
        assertEquals(350f, HeadingMath.normalizeDegrees(-10f), 0.001f)
        assertEquals(0f, HeadingMath.normalizeDegrees(720f), 0.001f)
        assertEquals(1f, HeadingMath.normalizeDegrees(361f), 0.001f)
    }
    @Test fun directionsIncludeSectorBoundaries() {
        assertEquals("Пн", HeadingMath.direction(359f))
        assertEquals("Пн", HeadingMath.direction(22.49f))
        assertEquals("Пн-Сх", HeadingMath.direction(22.5f))
        assertEquals("Сх", HeadingMath.direction(90f))
        assertEquals("Пд", HeadingMath.direction(180f))
        assertEquals("Зх", HeadingMath.direction(270f))
    }
    @Test fun animationCrossesNorthAlongShortestPath() {
        assertEquals(2f, HeadingMath.signedDifference(359f, 1f), 0.001f)
        assertEquals(-2f, HeadingMath.signedDifference(1f, 359f), 0.001f)
    }
    @Test fun thresholdIsStrictAndChecksBothSignsAndAxes() {
        assertFalse(HeadingMath.excessiveTilt(15f, -15f))
        assertTrue(HeadingMath.excessiveTilt(-15.1f, 0f))
        assertTrue(HeadingMath.excessiveTilt(0f, 16f))
    }
}
