package com.github.mikephil.charting.platform

import kotlin.math.abs

/**
 * The characters a [DecimalFormat] writes, like java.text.DecimalFormatSymbols. [getInstance] returns the
 * device locale's symbols, as java.text.DecimalFormatSymbols.getInstance() does on Android.
 */
public class DecimalFormatSymbols(
    public val decimalSeparator: Char = '.',
    public val groupingSeparator: Char = ',',
    public val minusSign: Char = '-',
    public val zeroDigit: Char = '0',
) {
    public companion object {
        /** English symbols: `1,234.5`. */
        public val ENGLISH: DecimalFormatSymbols = DecimalFormatSymbols()

        /** Symbols of the device's current locale. */
        public fun getInstance(): DecimalFormatSymbols = platformDecimalFormatSymbols()
    }
}

/** Symbols of the device's current formatting locale. */
internal expect fun platformDecimalFormatSymbols(): DecimalFormatSymbols

/**
 * Number formatting for the patterns the charts use, like java.text.DecimalFormat with the device locale's symbols:
 * `###,###,##0.00#` style patterns with grouping, minimum integer digits and minimum and maximum fraction
 * digits, rounded half even on the shortest decimal representation of the value, as Android's ICU does.
 */
public class DecimalFormat(pattern: String, private val symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance()) {

    private val groupingSize: Int
    private val minIntegerDigits: Int
    private val minFractionDigits: Int
    private val maxFractionDigits: Int

    /** Largest number of decimals the pattern shows. */
    public val maximumFractionDigits: Int get() = maxFractionDigits

    /** Smallest number of decimals the pattern shows. */
    public val minimumFractionDigits: Int get() = minFractionDigits
    private val prefix: String
    private val suffix: String

    init {
        val start = pattern.indexOfFirst { it == '#' || it == '0' || it == ',' || it == '.' }
        val end = pattern.indexOfLast { it == '#' || it == '0' || it == ',' || it == '.' }
        prefix = if (start > 0) pattern.substring(0, start) else ""
        suffix = if (end >= 0 && end < pattern.length - 1) pattern.substring(end + 1) else ""
        val body = if (start >= 0) pattern.substring(start, end + 1) else ""
        val dot = body.indexOf('.')
        val integerPart = if (dot >= 0) body.substring(0, dot) else body
        val fractionPart = if (dot >= 0) body.substring(dot + 1) else ""
        val lastComma = integerPart.lastIndexOf(',')
        groupingSize = if (lastComma >= 0) integerPart.length - lastComma - 1 else 0
        minIntegerDigits = integerPart.count { it == '0' }
        minFractionDigits = fractionPart.count { it == '0' }
        maxFractionDigits = fractionPart.count { it == '0' || it == '#' }
    }

    public fun format(number: Float): String = format(number.toDouble())

    public fun format(number: Long): String = format(number.toDouble())

    public fun format(number: Double): String {
        if (number.isNaN()) return "NaN"
        if (number.isInfinite()) return (if (number < 0) symbols.minusSign.toString() else "") + "∞"

        val (digits, exponent) = shortestDigits(abs(number))
        // digits d1 d2 ... dn with value 0.d1d2...dn * 10^exponent
        val rounded = roundHalfEven(digits, exponent, maxFractionDigits)
        val intDigits = rounded.first
        var fraction = rounded.second

        while (fraction.length > minFractionDigits && fraction.endsWith('0')) fraction = fraction.dropLast(1)

        var integer = intDigits.trimStart('0')
        if (integer.length < minIntegerDigits) integer = "0".repeat(minIntegerDigits - integer.length) + integer

        if (groupingSize > 0 && integer.length > groupingSize) {
            val sb = StringBuilder()
            for ((i, c) in integer.withIndex()) {
                if (i > 0 && (integer.length - i) % groupingSize == 0) sb.append(symbols.groupingSeparator)
                sb.append(c)
            }
            integer = sb.toString()
        }

        val isZero = intDigits.all { it == '0' } && fraction.all { it == '0' }
        val sign = if (number < 0 && !isZero) symbols.minusSign.toString() else ""
        val body = localizeDigits(if (fraction.isEmpty()) integer else integer + symbols.decimalSeparator + fraction)
        return sign + prefix + body + suffix
    }

    private fun localizeDigits(text: String): String {
        val zero = symbols.zeroDigit
        if (zero == '0') return text
        return buildString(text.length) { for (c in text) append(if (c in '0'..'9') zero + (c - '0') else c) }
    }

    private companion object {
        /** Shortest round-trip digits of a non-negative finite value as (digits, exponent): 0.digits * 10^exponent. */
        fun shortestDigits(value: Double): Pair<String, Int> {
            if (value == 0.0) return "0" to 1
            val s = value.toString().lowercase()
            val mantissa: String
            var exp = 0
            val e = s.indexOf('e')
            if (e >= 0) {
                mantissa = s.substring(0, e)
                exp = s.substring(e + 1).toInt()
            } else {
                mantissa = s
            }
            val dot = mantissa.indexOf('.')
            val intPart = if (dot >= 0) mantissa.substring(0, dot) else mantissa
            val fracPart = if (dot >= 0) mantissa.substring(dot + 1) else ""
            var digits = intPart + fracPart
            var pointPos = intPart.length + exp
            val leading = digits.length - digits.trimStart('0').length
            digits = digits.substring(leading)
            pointPos -= leading
            digits = digits.trimEnd('0')
            if (digits.isEmpty()) return "0" to 1
            return digits to pointPos
        }

        /** Rounds 0.digits * 10^exponent half even to [fractionDigits] decimals; returns (integer digits, fraction digits). */
        fun roundHalfEven(digits: String, exponent: Int, fractionDigits: Int): Pair<String, String> {
            // absolute digit string with the decimal point after `exponent` digits
            val keep = exponent + fractionDigits
            val all: String
            val pointIndex: Int
            if (exponent <= 0) {
                all = "0".repeat(-exponent + 1) + digits
                pointIndex = 1
            } else {
                all = if (digits.length < exponent) digits + "0".repeat(exponent - digits.length) else digits
                pointIndex = exponent
            }
            val keepCount = pointIndex + fractionDigits
            val kept = StringBuilder(if (all.length >= keepCount) all.substring(0, keepCount) else all + "0".repeat(keepCount - all.length))
            val rest = if (all.length > keepCount) all.substring(keepCount) else ""
            val roundUp = when {
                rest.isEmpty() -> false
                rest[0] > '5' -> true
                rest[0] < '5' -> false
                rest.substring(1).any { it != '0' } -> true
                else -> kept.isNotEmpty() && (kept.last() - '0') % 2 == 1
            }
            if (keep < 0 && !roundUp) return "0" to "0".repeat(fractionDigits)
            var result = kept.toString()
            if (roundUp) {
                val chars = result.toCharArray()
                var i = chars.size - 1
                while (i >= 0) {
                    if (chars[i] == '9') {
                        chars[i] = '0'
                        i--
                    } else {
                        chars[i] = chars[i] + 1
                        break
                    }
                }
                result = chars.concatToString()
                if (i < 0) {
                    result = "1$result"
                    return result.substring(0, pointIndex + 1) to result.substring(pointIndex + 1)
                }
            }
            return result.substring(0, pointIndex) to result.substring(pointIndex)
        }
    }
}
