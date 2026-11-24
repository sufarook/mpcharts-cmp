package com.github.mikephil.charting.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.currentLocale

internal actual fun platformDecimalFormatSymbols(): DecimalFormatSymbols {
    val formatter = NSNumberFormatter().apply { locale = NSLocale.currentLocale }
    val zero = formatter.stringFromNumber(NSNumber(int = 0))?.firstOrNull() ?: '0'
    return DecimalFormatSymbols(
        decimalSeparator = formatter.decimalSeparator.firstOrNull() ?: '.',
        groupingSeparator = formatter.groupingSeparator.firstOrNull() ?: ',',
        minusSign = formatter.minusSign.firstOrNull() ?: '-',
        zeroDigit = zero,
    )
}
