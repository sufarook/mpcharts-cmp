package com.github.mikephil.charting.port

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.platform.Bitmap
import com.github.mikephil.charting.platform.Color
import com.github.mikephil.charting.platform.ForegroundColorSpan
import com.github.mikephil.charting.platform.Layout
import com.github.mikephil.charting.platform.RelativeSizeSpan
import com.github.mikephil.charting.platform.SkiaBitmap
import com.github.mikephil.charting.platform.SkiaFonts
import com.github.mikephil.charting.platform.SpannableString
import com.github.mikephil.charting.platform.StaticLayout
import com.github.mikephil.charting.platform.TextPaint
import com.github.mikephil.charting.utils.Utils
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** Checks the parts the port adds around MPAndroidChart's code: spans, chart images and backgrounds. */
class PortFeaturesTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadFont() {
            Utils.init(2f)
            if (!SkiaFonts.isLoaded) {
                SkiaFonts.loadRoboto(File("src/commonMain/composeResources/files/Roboto-Regular.ttf").readBytes())
            }
        }
    }

    private fun layout(text: CharSequence, width: Int = 1000) = StaticLayout.Builder
        .obtain(text, 0, text.length, TextPaint().apply { textSize = 20f }, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setIncludePad(false)
        .build()

    @Test
    fun relativeSizeSpanMakesItsLineTallerAndWider() {
        val plain = layout("Title\nline")
        val styled = SpannableString("Title\nline").apply { setSpan(RelativeSizeSpan(2f), 0, 5, 0) }
        val big = layout(styled)
        assertEquals(2, big.lineCount)
        assertTrue(big.height > plain.height)
        assertEquals(plain.getLineWidth(0) * 2f, big.getLineWidth(0), plain.getLineWidth(0) * 0.05f) // advances round per size
        assertEquals(plain.getLineWidth(1), big.getLineWidth(1), 0.01f)
    }

    @Test
    fun spannedTextBreaksLinesLikePlainText() {
        val text = "developed by Philipp Jahoda and others"
        val styled = SpannableString(text).apply { setSpan(ForegroundColorSpan(Color.RED), 0, 9, 0) }
        assertEquals(layout(text, 200).lineCount, layout(styled, 200).lineCount)
        assertEquals(layout(text, 200).height, layout(styled, 200).height)
    }

    @Test
    fun subSequenceKeepsClippedSpans() {
        val span = ForegroundColorSpan(Color.RED)
        val s = SpannableString("abcdef").apply { setSpan(span, 1, 4, 0) }
        assertSame(s, s.subSequence(0, 6))
        val sub = s.subSequence(2, 6) as SpannableString
        assertEquals("cdef", sub.toString())
        assertEquals(0, sub.getSpanStart(span))
        assertEquals(2, sub.getSpanEnd(span))
    }

    @Test
    fun toBitmapDrawsBackgroundAndRestoresSize() {
        val chart = BarChart()
        chart.data = BarData(BarDataSet(listOf(BarEntry(0f, 1f), BarEntry(1f, 2f)), "set"))
        chart.setSize(300, 200)
        chart.setBackgroundColor(Color.rgb(60, 65, 82))

        val bitmap = chart.toBitmap(600, 400) as SkiaBitmap
        assertEquals(600, bitmap.width)
        assertEquals(400, bitmap.height)
        assertEquals(300, chart.width)
        assertEquals(200, chart.height)

        val png = bitmap.compress(Bitmap.CompressFormat.PNG, 100)
        assertArrayEquals(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()), png.copyOf(4))
        val image = org.jetbrains.skia.Image.makeFromEncoded(png)
        val pixels = org.jetbrains.skia.Bitmap.makeFromImage(image)
        assertEquals(Color.rgb(60, 65, 82), pixels.getColor(1, 1))
    }

    @Test
    fun toBitmapWithoutBackgroundIsWhite() {
        val chart = BarChart()
        chart.setSize(100, 100)
        val png = chart.toBitmap().compress(Bitmap.CompressFormat.PNG, 100)
        val pixels = org.jetbrains.skia.Bitmap.makeFromImage(org.jetbrains.skia.Image.makeFromEncoded(png))
        assertEquals(Color.WHITE, pixels.getColor(0, 0))
    }
}
