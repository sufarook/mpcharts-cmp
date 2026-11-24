package com.github.mikephil.charting.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class DecimalFormatTest {
    @Test
    fun formatsLikeAndroid() {
        val oneDecimal = DecimalFormat("###,###,###,##0.0", DecimalFormatSymbols.ENGLISH)
        assertEquals("1,234.5", oneDecimal.format(1234.5))
        assertEquals("0.2", oneDecimal.format(0.15)) // half even on the shortest decimal form, as ICU does
        assertEquals("-3.0", oneDecimal.format(-3.0))
        assertEquals("1,000,000", DecimalFormat("###,###,##0", DecimalFormatSymbols.ENGLISH).format(999999.6))
        assertEquals("12.5 %", DecimalFormat("###,###,##0.0", DecimalFormatSymbols.ENGLISH).format(12.46) + " %")
    }

    @Test
    fun usesLocaleSymbols() {
        val german = DecimalFormatSymbols(decimalSeparator = ',', groupingSeparator = '.')
        assertEquals("1.234,5", DecimalFormat("###,###,###,##0.0", german).format(1234.5))
        val arabic = DecimalFormatSymbols(decimalSeparator = '٫', groupingSeparator = '٬', zeroDigit = '٠')
        assertEquals("١٬٢٣٤٫٥", DecimalFormat("###,###,###,##0.0", arabic).format(1234.5))
    }
}
