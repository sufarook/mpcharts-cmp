package com.github.mikephil.charting.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class MatrixTest {
    @Test
    fun scaleTranslateKeepsAxesIndependent() {
        // The y axis renderer leaves x unset, so x can grow to infinity; y must stay correct, as with Skia.
        val m = Matrix()
        m.postScale(2f, -3f)
        m.postTranslate(10f, 20f)
        val pts = floatArrayOf(Float.POSITIVE_INFINITY, 4f)
        m.mapPoints(pts)
        assertEquals(8f, pts[1])
    }

    @Test
    fun invertRoundTrips() {
        val m = Matrix()
        m.postScale(4f, 0.5f, 10f, 10f)
        m.postTranslate(-3f, 7f)
        val inverse = Matrix()
        m.invert(inverse)
        val pts = floatArrayOf(12f, -5f)
        m.mapPoints(pts)
        inverse.mapPoints(pts)
        assertEquals(12f, pts[0], 1e-4f)
        assertEquals(-5f, pts[1], 1e-4f)
    }
}

class ColorTest {
    @Test
    fun hsvRoundTrips() {
        val hsv = FloatArray(3)
        for (color in listOf(Color.rgb(193, 37, 82), Color.rgb(255, 102, 0), Color.rgb(0, 0, 0), Color.rgb(200, 200, 200))) {
            Color.colorToHSV(color, hsv)
            assertEquals(color, Color.HSVToColor(hsv))
        }
        Color.RGBToHSV(255, 0, 0, hsv)
        assertEquals(0f, hsv[0])
        assertEquals(1f, hsv[1])
    }
}
