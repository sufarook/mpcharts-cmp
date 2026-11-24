package com.github.mikephil.charting.platform

internal actual fun platformDecimalFormatSymbols(): DecimalFormatSymbols {
    val s = java.text.DecimalFormatSymbols.getInstance()
    return DecimalFormatSymbols(s.decimalSeparator, s.groupingSeparator, s.minusSign, s.zeroDigit)
}
