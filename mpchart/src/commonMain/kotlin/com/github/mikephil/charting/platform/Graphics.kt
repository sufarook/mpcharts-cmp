package com.github.mikephil.charting.platform

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Multiplatform stand-ins for the android.graphics classes MPAndroidChart draws with. They keep the Android
 * names, signatures and semantics so the chart and renderer code is the original code. The drawing itself is
 * done by a platform Canvas: android.graphics.Canvas on Android and the Skia canvas on desktop and iOS, which
 * is the engine android.graphics runs on, so the pixels match.
 */

/** Colors as ARGB ints, like android.graphics.Color. */
public object Color {
    public const val BLACK: Int = 0xFF000000.toInt()
    public const val DKGRAY: Int = 0xFF444444.toInt()
    public const val GRAY: Int = 0xFF888888.toInt()
    public const val LTGRAY: Int = 0xFFCCCCCC.toInt()
    public const val WHITE: Int = 0xFFFFFFFF.toInt()
    public const val RED: Int = 0xFFFF0000.toInt()
    public const val GREEN: Int = 0xFF00FF00.toInt()
    public const val BLUE: Int = 0xFF0000FF.toInt()
    public const val YELLOW: Int = 0xFFFFFF00.toInt()
    public const val CYAN: Int = 0xFF00FFFF.toInt()
    public const val MAGENTA: Int = 0xFFFF00FF.toInt()
    public const val TRANSPARENT: Int = 0

    public fun alpha(color: Int): Int = color ushr 24
    public fun red(color: Int): Int = (color shr 16) and 0xFF
    public fun green(color: Int): Int = (color shr 8) and 0xFF
    public fun blue(color: Int): Int = color and 0xFF

    public fun rgb(red: Int, green: Int, blue: Int): Int = argb(255, red, green, blue)

    public fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    /** Converts RGB (0..255 each) to hue (0..360), saturation and value (0..1), like Android (Skia's SkRGBToHSV). */
    public fun RGBToHSV(red: Int, green: Int, blue: Int, hsv: FloatArray) {
        val min = minOf(red, green, blue)
        val max = maxOf(red, green, blue)
        val delta = max - min
        val v = max / 255f
        if (delta == 0) {
            hsv[0] = 0f
            hsv[1] = 0f
            hsv[2] = v
            return
        }
        val s = delta.toFloat() / max
        var h = when (max) {
            red -> (green - blue).toFloat() / delta
            green -> 2f + (blue - red).toFloat() / delta
            else -> 4f + (red - green).toFloat() / delta
        }
        h *= 60f
        if (h < 0f) h += 360f
        hsv[0] = h
        hsv[1] = s
        hsv[2] = v
    }

    /** Converts an ARGB color to HSV, see [RGBToHSV]. */
    public fun colorToHSV(color: Int, hsv: FloatArray): Unit = RGBToHSV(red(color), green(color), blue(color), hsv)

    /** Converts HSV to an opaque color, like Android (Skia's SkHSVToColor). */
    public fun HSVToColor(hsv: FloatArray): Int = HSVToColor(0xFF, hsv)

    /** Converts HSV to a color with [alpha], like Android (Skia's SkHSVToColor). */
    public fun HSVToColor(alpha: Int, hsv: FloatArray): Int {
        fun round(x: Float): Int = kotlin.math.floor(x + 0.5f).toInt()
        val s = hsv[1].coerceIn(0f, 1f)
        val v = hsv[2].coerceIn(0f, 1f)
        val vByte = round(v * 255f)
        if (kotlin.math.abs(s) <= 1f / 4096f) return argb(alpha, vByte, vByte, vByte)
        val hx = if (hsv[0] < 0f || hsv[0] >= 360f) 0f else hsv[0] / 60f
        val w = kotlin.math.floor(hx)
        val f = hx - w
        val p = round((1f - s) * v * 255f)
        val q = round((1f - s * f) * v * 255f)
        val t = round((1f - s * (1f - f)) * v * 255f)
        return when (w.toInt()) {
            0 -> argb(alpha, vByte, t, p)
            1 -> argb(alpha, q, vByte, p)
            2 -> argb(alpha, p, vByte, t)
            3 -> argb(alpha, p, q, vByte)
            4 -> argb(alpha, t, p, vByte)
            else -> argb(alpha, vByte, p, q)
        }
    }

    /** Parses `#RRGGBB` or `#AARRGGBB`. */
    public fun parseColor(colorString: String): Int {
        require(colorString.startsWith("#")) { "Unknown color: $colorString" }
        val value = colorString.substring(1).toLong(16)
        return when (colorString.length) {
            7 -> (value or 0xFF000000L).toInt()
            9 -> value.toInt()
            else -> throw IllegalArgumentException("Unknown color: $colorString")
        }
    }
}

/** Integer rectangle, like android.graphics.Rect. */
public class Rect(
    public var left: Int = 0,
    public var top: Int = 0,
    public var right: Int = 0,
    public var bottom: Int = 0,
) {
    public fun set(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    public fun set(src: Rect): Unit = set(src.left, src.top, src.right, src.bottom)
    public fun width(): Int = right - left
    public fun height(): Int = bottom - top
    public fun isEmpty(): Boolean = left >= right || top >= bottom
    override fun equals(other: Any?): Boolean =
        other is Rect && left == other.left && top == other.top && right == other.right && bottom == other.bottom
    override fun hashCode(): Int = ((left * 31 + top) * 31 + right) * 31 + bottom
    override fun toString(): String = "Rect($left, $top - $right, $bottom)"
}

/** Float rectangle, like android.graphics.RectF. */
public class RectF(
    public var left: Float = 0f,
    public var top: Float = 0f,
    public var right: Float = 0f,
    public var bottom: Float = 0f,
) {
    public constructor(src: RectF) : this(src.left, src.top, src.right, src.bottom)

    public fun set(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    public fun set(src: RectF): Unit = set(src.left, src.top, src.right, src.bottom)
    public fun setEmpty(): Unit = set(0f, 0f, 0f, 0f)
    public fun width(): Float = right - left
    public fun height(): Float = bottom - top
    public fun centerX(): Float = (left + right) * 0.5f
    public fun centerY(): Float = (top + bottom) * 0.5f
    public fun isEmpty(): Boolean = !(left < right && top < bottom)
    public fun contains(x: Float, y: Float): Boolean = left < right && top < bottom && x >= left && x < right && y >= top && y < bottom

    public fun inset(dx: Float, dy: Float) {
        left += dx
        top += dy
        right -= dx
        bottom -= dy
    }

    public fun offset(dx: Float, dy: Float) {
        left += dx
        top += dy
        right += dx
        bottom += dy
    }

    public fun union(r: RectF) {
        if (r.left >= r.right || r.top >= r.bottom) return
        if (left < right && top < bottom) {
            left = min(left, r.left)
            top = min(top, r.top)
            right = max(right, r.right)
            bottom = max(bottom, r.bottom)
        } else {
            set(r)
        }
    }

    public fun intersects(l: Float, t: Float, r: Float, b: Float): Boolean = left < r && l < right && top < b && t < bottom

    public fun sort() {
        if (left > right) left = right.also { right = left }
        if (top > bottom) top = bottom.also { bottom = top }
    }

    override fun equals(other: Any?): Boolean =
        other is RectF && left == other.left && top == other.top && right == other.right && bottom == other.bottom
    override fun hashCode(): Int = ((left.hashCode() * 31 + top.hashCode()) * 31 + right.hashCode()) * 31 + bottom.hashCode()
    override fun toString(): String = "RectF($left, $top, $right, $bottom)"
}

/** Font family and style, like android.graphics.Typeface. [weight] follows CSS, 400 regular and 700 bold. */
public class Typeface private constructor(
    public val familyName: String?,
    public val weight: Int,
    public val isItalic: Boolean,
) {
    public val style: Int get() = (if (isBold) BOLD else 0) or (if (isItalic) ITALIC else 0)
    public val isBold: Boolean get() = weight >= 600

    override fun equals(other: Any?): Boolean =
        other is Typeface && familyName == other.familyName && weight == other.weight && isItalic == other.isItalic
    override fun hashCode(): Int = (familyName.hashCode() * 31 + weight) * 31 + isItalic.hashCode()

    public companion object {
        public const val NORMAL: Int = 0
        public const val BOLD: Int = 1
        public const val ITALIC: Int = 2
        public const val BOLD_ITALIC: Int = 3

        public val DEFAULT: Typeface = Typeface(null, 400, false)
        public val DEFAULT_BOLD: Typeface = Typeface(null, 700, false)
        public val SANS_SERIF: Typeface = Typeface("sans-serif", 400, false)
        public val SERIF: Typeface = Typeface("serif", 400, false)
        public val MONOSPACE: Typeface = Typeface("monospace", 400, false)

        /** Like Android, `sans-serif-light`, `-medium`, `-black` and `-thin` select a Roboto weight. */
        public fun create(familyName: String?, style: Int): Typeface {
            val weight = when (familyName) {
                "sans-serif-thin" -> 100
                "sans-serif-light" -> 300
                "sans-serif-medium" -> 500
                "sans-serif-black" -> 900
                else -> 400
            }
            val family = if (familyName.isNullOrEmpty()) null else familyName
            val bold = style and BOLD != 0
            return Typeface(family, if (bold) max(weight, 700) else weight, style and ITALIC != 0)
        }

        public fun create(family: Typeface?, style: Int): Typeface {
            val base = family ?: DEFAULT
            val bold = style and BOLD != 0
            val weight = when {
                bold -> max(base.weight, 700)
                base.isBold -> 400
                else -> base.weight
            }
            return Typeface(base.familyName, weight, style and ITALIC != 0)
        }

        /** Typeface of the given family at a CSS [weight] from 100 to 900. */
        public fun create(family: Typeface?, weight: Int, italic: Boolean): Typeface =
            Typeface((family ?: DEFAULT).familyName, weight.coerceIn(1, 1000), italic)
    }
}

/** Base class of path effects. Only [DashPathEffect] is used by the charts. */
public open class PathEffect

/** Dashes a stroke, like android.graphics.DashPathEffect. */
public class DashPathEffect(public val intervals: FloatArray, public val phase: Float) : PathEffect()

/** Base class of shaders, like android.graphics.Shader. */
public open class Shader {
    public enum class TileMode { CLAMP, REPEAT, MIRROR, DECAL }
}

/** Linear gradient, like android.graphics.LinearGradient. */
public class LinearGradient(
    public val x0: Float,
    public val y0: Float,
    public val x1: Float,
    public val y1: Float,
    public val colors: IntArray,
    public val positions: FloatArray?,
    public val tile: TileMode,
) : Shader() {
    public constructor(x0: Float, y0: Float, x1: Float, y1: Float, color0: Int, color1: Int, tile: TileMode) :
        this(x0, y0, x1, y1, intArrayOf(color0, color1), null, tile)
}

/** Porter-Duff blend modes, like android.graphics.PorterDuff. */
public object PorterDuff {
    public enum class Mode { CLEAR, SRC, DST, SRC_OVER, DST_OVER, SRC_IN, DST_IN, SRC_OUT, DST_OUT, SRC_ATOP, DST_ATOP, XOR }
}

/** Base class of transfer modes, like android.graphics.Xfermode. */
public open class Xfermode

/** Blends with a [PorterDuff.Mode], like android.graphics.PorterDuffXfermode. */
public class PorterDuffXfermode(public val mode: PorterDuff.Mode) : Xfermode()

/**
 * Drawing style and text settings, like android.graphics.Paint. Text is measured by the platform font engine:
 * android.graphics.Paint on Android, Skia on desktop and iOS.
 */
public open class Paint(flags: Int = 0) {

    /** Changes on every property change, so platform canvases can cache their converted paint. */
    internal var version: Int = 0
        private set

    /** Cache slot for the platform canvas implementation. */
    internal var platformCache: Any? = null
    internal var platformCacheVersion: Int = -1

    private fun changed() {
        version++
    }

    public constructor(paint: Paint) : this(0) {
        set(paint)
    }

    public var flags: Int = flags
        set(value) {
            field = value
            changed()
        }

    public var isAntiAlias: Boolean
        get() = flags and ANTI_ALIAS_FLAG != 0
        set(value) {
            flags = if (value) flags or ANTI_ALIAS_FLAG else flags and ANTI_ALIAS_FLAG.inv()
        }

    public var color: Int = Color.BLACK
        set(value) {
            field = value
            changed()
        }

    /** Alpha of [color], 0 to 255. Setting it keeps the color's RGB. */
    public var alpha: Int
        get() = Color.alpha(color)
        set(value) {
            color = (color and 0x00FFFFFF) or (value.coerceIn(0, 255) shl 24)
        }

    public fun setARGB(a: Int, r: Int, g: Int, b: Int) {
        color = Color.argb(a, r, g, b)
    }

    public var style: Style = Style.FILL
        set(value) {
            field = value
            changed()
        }

    public var strokeWidth: Float = 0f
        set(value) {
            field = value
            changed()
        }

    public var strokeCap: Cap = Cap.BUTT
        set(value) {
            field = value
            changed()
        }

    public var strokeJoin: Join = Join.MITER
        set(value) {
            field = value
            changed()
        }

    public var strokeMiter: Float = 4f
        set(value) {
            field = value
            changed()
        }

    /** Text size in pixels. Android's default is 12. */
    public var textSize: Float = 12f
        set(value) {
            field = value
            changed()
        }

    public var textAlign: Align = Align.LEFT
        set(value) {
            field = value
            changed()
        }

    public var typeface: Typeface? = null
        set(value) {
            field = value
            changed()
        }

    public var pathEffect: PathEffect? = null
        set(value) {
            field = value
            changed()
        }

    public var shader: Shader? = null
        set(value) {
            field = value
            changed()
        }

    public var xfermode: Xfermode? = null
        set(value) {
            field = value
            changed()
        }

    public fun set(src: Paint) {
        flags = src.flags
        color = src.color
        style = src.style
        strokeWidth = src.strokeWidth
        strokeCap = src.strokeCap
        strokeJoin = src.strokeJoin
        strokeMiter = src.strokeMiter
        textSize = src.textSize
        textAlign = src.textAlign
        typeface = src.typeface
        pathEffect = src.pathEffect
        shader = src.shader
        xfermode = src.xfermode
    }

    public fun reset() {
        set(Paint())
    }

    /** Advance width of [text] in pixels. */
    public open fun measureText(text: String): Float = if (text.isEmpty()) 0f else PlatformText.measureText(this, text)

    public open fun measureText(text: CharSequence, start: Int, end: Int): Float = measureText(text.subSequence(start, end).toString())

    /** Tight pixel bounds of the glyphs of `text[start, end)` relative to the origin, rounded out to whole pixels. */
    public open fun getTextBounds(text: String, start: Int, end: Int, bounds: Rect) {
        val sub = text.substring(start, end)
        if (sub.isEmpty()) {
            bounds.set(0, 0, 0, 0)
            return
        }
        PlatformText.getTextBounds(this, sub, bounds)
    }

    /** Fills [metrics] when given and returns the recommended line spacing, like Paint.getFontMetrics. */
    public open fun getFontMetrics(metrics: FontMetrics?): Float {
        val m = metrics ?: FontMetrics()
        PlatformText.getFontMetrics(this, m)
        return m.descent - m.ascent + m.leading
    }

    public val fontMetrics: FontMetrics
        get() = FontMetrics().also { getFontMetrics(it) }

    /** Recommended line spacing in pixels. */
    public val fontSpacing: Float
        get() = getFontMetrics(null)

    /** Distance above the baseline, negative. */
    public fun ascent(): Float = fontMetrics.ascent

    /** Distance below the baseline, positive. */
    public fun descent(): Float = fontMetrics.descent

    public enum class Style { FILL, STROKE, FILL_AND_STROKE }
    public enum class Align { LEFT, CENTER, RIGHT }
    public enum class Cap { BUTT, ROUND, SQUARE }
    public enum class Join { MITER, ROUND, BEVEL }

    /** Font metrics in pixels relative to the baseline, like Paint.FontMetrics. */
    public class FontMetrics {
        public var top: Float = 0f
        public var ascent: Float = 0f
        public var descent: Float = 0f
        public var bottom: Float = 0f
        public var leading: Float = 0f
    }

    public companion object {
        public const val ANTI_ALIAS_FLAG: Int = 0x01
        public const val FILTER_BITMAP_FLAG: Int = 0x02
        public const val DITHER_FLAG: Int = 0x04
    }
}

/** Paint used for laid out text, like android.text.TextPaint. */
public open class TextPaint(flags: Int = 0) : Paint(flags)

/**
 * 3x3 transformation matrix with the semantics of android.graphics.Matrix, values stored row by row:
 * `[scaleX, skewX, transX, skewY, scaleY, transY, persp0, persp1, persp2]`.
 */
public class Matrix() {
    private val v = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

    /** Changes on every modification, so platform canvases can cache conversions. */
    internal var version: Int = 0
        private set

    public constructor(src: Matrix?) : this() {
        if (src != null) set(src)
    }

    private fun changed() {
        version++
    }

    public fun isIdentity(): Boolean =
        v[0] == 1f && v[1] == 0f && v[2] == 0f && v[3] == 0f && v[4] == 1f && v[5] == 0f && v[6] == 0f && v[7] == 0f && v[8] == 1f

    public fun reset() {
        setIdentity(v)
        changed()
    }

    public fun set(src: Matrix?) {
        if (src == null) reset() else {
            src.v.copyInto(v)
            changed()
        }
    }

    public fun getValues(values: FloatArray) {
        v.copyInto(values, 0, 0, 9)
    }

    public fun setValues(values: FloatArray) {
        values.copyInto(v, 0, 0, 9)
        changed()
    }

    public fun setScale(sx: Float, sy: Float) {
        setIdentity(v)
        v[MSCALE_X] = sx
        v[MSCALE_Y] = sy
        changed()
    }

    public fun setScale(sx: Float, sy: Float, px: Float, py: Float) {
        setIdentity(v)
        v[MSCALE_X] = sx
        v[MSCALE_Y] = sy
        v[MTRANS_X] = px - sx * px
        v[MTRANS_Y] = py - sy * py
        changed()
    }

    public fun setTranslate(dx: Float, dy: Float) {
        setIdentity(v)
        v[MTRANS_X] = dx
        v[MTRANS_Y] = dy
        changed()
    }

    public fun setConcat(a: Matrix, b: Matrix): Boolean {
        val r = multiply(a.v, b.v)
        r.copyInto(v)
        changed()
        return true
    }

    public fun preTranslate(dx: Float, dy: Float): Boolean = preConcatValues(translateValues(dx, dy))
    public fun preScale(sx: Float, sy: Float): Boolean = preConcatValues(scaleValues(sx, sy, 0f, 0f))
    public fun preScale(sx: Float, sy: Float, px: Float, py: Float): Boolean = preConcatValues(scaleValues(sx, sy, px, py))
    public fun preConcat(other: Matrix): Boolean = preConcatValues(other.v)
    public fun postTranslate(dx: Float, dy: Float): Boolean = postConcatValues(translateValues(dx, dy))
    public fun postScale(sx: Float, sy: Float): Boolean = postConcatValues(scaleValues(sx, sy, 0f, 0f))
    public fun postScale(sx: Float, sy: Float, px: Float, py: Float): Boolean = postConcatValues(scaleValues(sx, sy, px, py))
    public fun postConcat(other: Matrix): Boolean = postConcatValues(other.v)

    private fun preConcatValues(other: FloatArray): Boolean {
        multiply(v, other).copyInto(v)
        changed()
        return true
    }

    private fun postConcatValues(other: FloatArray): Boolean {
        multiply(other, v).copyInto(v)
        changed()
        return true
    }

    /** Writes the inverse into [inverse] and returns true, or returns false when this matrix cannot be inverted. */
    public fun invert(inverse: Matrix): Boolean {
        val a = v.map { it.toDouble() }
        val det = a[0] * (a[4] * a[8] - a[5] * a[7]) - a[1] * (a[3] * a[8] - a[5] * a[6]) + a[2] * (a[3] * a[7] - a[4] * a[6])
        if (det == 0.0 || !det.isFinite()) return false
        val inv = 1.0 / det
        val r = doubleArrayOf(
            (a[4] * a[8] - a[5] * a[7]) * inv,
            (a[2] * a[7] - a[1] * a[8]) * inv,
            (a[1] * a[5] - a[2] * a[4]) * inv,
            (a[5] * a[6] - a[3] * a[8]) * inv,
            (a[0] * a[8] - a[2] * a[6]) * inv,
            (a[2] * a[3] - a[0] * a[5]) * inv,
            (a[3] * a[7] - a[4] * a[6]) * inv,
            (a[1] * a[6] - a[0] * a[7]) * inv,
            (a[0] * a[4] - a[1] * a[3]) * inv,
        )
        for (i in 0 until 9) inverse.v[i] = r[i].toFloat()
        inverse.changed()
        return true
    }

    /** Maps x, y pairs in place. */
    public fun mapPoints(pts: FloatArray) {
        mapPoints(pts, 0, pts, 0, pts.size / 2)
    }

    public fun mapPoints(dst: FloatArray, src: FloatArray) {
        mapPoints(dst, 0, src, 0, src.size / 2)
    }

    /**
     * Maps points like Skia's SkMatrix::mapPoints, which Android uses: a matrix without skew or perspective maps x
     * and y independently, so a non-finite x never leaks into y. MPAndroidChart relies on this, for example in
     * the y axis renderer, which leaves the x half of its position buffer unset.
     */
    public fun mapPoints(dst: FloatArray, dstIndex: Int, src: FloatArray, srcIndex: Int, pointCount: Int) {
        val perspective = v[6] != 0f || v[7] != 0f || v[8] != 1f
        val skew = v[1] != 0f || v[3] != 0f
        for (i in 0 until pointCount) {
            val x = src[srcIndex + i * 2]
            val y = src[srcIndex + i * 2 + 1]
            val nx: Float
            val ny: Float
            if (!perspective && !skew) {
                nx = x * v[0] + v[2]
                ny = y * v[4] + v[5]
            } else if (!perspective) {
                nx = v[0] * x + v[1] * y + v[2]
                ny = v[3] * x + v[4] * y + v[5]
            } else {
                val w = v[6] * x + v[7] * y + v[8]
                val iw = if (w != 0f) 1f / w else 0f
                nx = (v[0] * x + v[1] * y + v[2]) * iw
                ny = (v[3] * x + v[4] * y + v[5]) * iw
            }
            dst[dstIndex + i * 2] = nx
            dst[dstIndex + i * 2 + 1] = ny
        }
    }

    /** Maps [rect] in place to the bounds of its transformed corners. Returns true when it stays a rectangle. */
    public fun mapRect(rect: RectF): Boolean = mapRect(rect, rect)

    public fun mapRect(dst: RectF, src: RectF): Boolean {
        val pts = floatArrayOf(src.left, src.top, src.right, src.top, src.right, src.bottom, src.left, src.bottom)
        mapPoints(pts)
        dst.set(
            min(min(pts[0], pts[2]), min(pts[4], pts[6])),
            min(min(pts[1], pts[3]), min(pts[5], pts[7])),
            max(max(pts[0], pts[2]), max(pts[4], pts[6])),
            max(max(pts[1], pts[3]), max(pts[5], pts[7])),
        )
        return rectStaysRect()
    }

    public fun rectStaysRect(): Boolean = v[6] == 0f && v[7] == 0f && ((v[1] == 0f && v[3] == 0f) || (v[0] == 0f && v[4] == 0f))

    override fun equals(other: Any?): Boolean = other is Matrix && v.contentEquals(other.v)
    override fun hashCode(): Int = v.contentHashCode()
    override fun toString(): String = "Matrix${v.contentToString()}"

    public companion object {
        public const val MSCALE_X: Int = 0
        public const val MSKEW_X: Int = 1
        public const val MTRANS_X: Int = 2
        public const val MSKEW_Y: Int = 3
        public const val MSCALE_Y: Int = 4
        public const val MTRANS_Y: Int = 5
        public const val MPERSP_0: Int = 6
        public const val MPERSP_1: Int = 7
        public const val MPERSP_2: Int = 8

        private fun setIdentity(m: FloatArray) {
            for (i in 0 until 9) m[i] = 0f
            m[0] = 1f
            m[4] = 1f
            m[8] = 1f
        }

        private fun translateValues(dx: Float, dy: Float) = floatArrayOf(1f, 0f, dx, 0f, 1f, dy, 0f, 0f, 1f)
        private fun scaleValues(sx: Float, sy: Float, px: Float, py: Float) =
            floatArrayOf(sx, 0f, px - sx * px, 0f, sy, py - sy * py, 0f, 0f, 1f)

        /** Returns a * b. */
        private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
            val r = FloatArray(9)
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    r[row * 3 + col] = a[row * 3] * b[col] + a[row * 3 + 1] * b[3 + col] + a[row * 3 + 2] * b[6 + col]
                }
            }
            return r
        }
    }
}

/**
 * Geometric path, like android.graphics.Path. It records its commands; a platform canvas replays them into a
 * native path, so arcs, round rects and transforms behave exactly as on the platform.
 */
public class Path() {
    internal val commands: MutableList<PathCommand> = mutableListOf()

    /** Changes on every modification, so platform canvases can cache the native path. */
    internal var version: Int = 0
        private set

    internal var platformCache: Any? = null
    internal var platformCacheVersion: Int = -1

    public var fillType: FillType = FillType.WINDING
        set(value) {
            field = value
            version++
        }

    public constructor(src: Path) : this() {
        set(src)
    }

    private fun add(command: PathCommand) {
        commands.add(command)
        version++
    }

    public fun set(src: Path) {
        commands.clear()
        commands.addAll(src.commands)
        fillType = src.fillType
        version++
    }

    public fun reset() {
        commands.clear()
        fillType = FillType.WINDING
        version++
    }

    public fun rewind() {
        commands.clear()
        version++
    }

    public val isEmpty: Boolean get() = commands.none { it !is PathCommand.Transform }

    public fun moveTo(x: Float, y: Float): Unit = add(PathCommand.MoveTo(x, y))
    public fun lineTo(x: Float, y: Float): Unit = add(PathCommand.LineTo(x, y))
    public fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float): Unit = add(PathCommand.QuadTo(x1, y1, x2, y2))
    public fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Unit = add(PathCommand.CubicTo(x1, y1, x2, y2, x3, y3))
    public fun close(): Unit = add(PathCommand.Close)

    public fun arcTo(oval: RectF, startAngle: Float, sweepAngle: Float, forceMoveTo: Boolean = false): Unit =
        arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo)

    public fun arcTo(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float, forceMoveTo: Boolean): Unit =
        add(PathCommand.ArcTo(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo))

    public fun addArc(oval: RectF, startAngle: Float, sweepAngle: Float): Unit =
        add(PathCommand.AddArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle))

    public fun addCircle(x: Float, y: Float, radius: Float, dir: Direction): Unit = add(PathCommand.AddCircle(x, y, radius, dir))

    public fun addOval(oval: RectF, dir: Direction): Unit = add(PathCommand.AddOval(oval.left, oval.top, oval.right, oval.bottom, dir))

    public fun addRect(left: Float, top: Float, right: Float, bottom: Float, dir: Direction): Unit =
        add(PathCommand.AddRect(left, top, right, bottom, dir))

    public fun addRect(rect: RectF, dir: Direction): Unit = addRect(rect.left, rect.top, rect.right, rect.bottom, dir)

    public fun addRoundRect(left: Float, top: Float, right: Float, bottom: Float, radii: FloatArray, dir: Direction): Unit =
        add(PathCommand.AddRoundRect(left, top, right, bottom, radii.copyOf(), dir))

    public fun addRoundRect(rect: RectF, radii: FloatArray, dir: Direction): Unit =
        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radii, dir)

    public fun addRoundRect(rect: RectF, rx: Float, ry: Float, dir: Direction): Unit =
        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, floatArrayOf(rx, ry, rx, ry, rx, ry, rx, ry), dir)

    /** Appends a copy of [src], like Path.addPath. */
    public fun addPath(src: Path): Unit = add(PathCommand.AddPath(Path(src)))

    /** Transforms all points recorded so far, like Path.transform. */
    public fun transform(matrix: Matrix) {
        val values = FloatArray(9)
        matrix.getValues(values)
        add(PathCommand.Transform(values))
    }

    public enum class Direction { CW, CCW }
    public enum class FillType { WINDING, EVEN_ODD, INVERSE_WINDING, INVERSE_EVEN_ODD }
}

/** One recorded [Path] command. */
internal sealed class PathCommand {
    class MoveTo(val x: Float, val y: Float) : PathCommand()
    class LineTo(val x: Float, val y: Float) : PathCommand()
    class QuadTo(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : PathCommand()
    class CubicTo(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float) : PathCommand()
    data object Close : PathCommand()
    class ArcTo(val l: Float, val t: Float, val r: Float, val b: Float, val start: Float, val sweep: Float, val forceMoveTo: Boolean) : PathCommand()
    class AddArc(val l: Float, val t: Float, val r: Float, val b: Float, val start: Float, val sweep: Float) : PathCommand()
    class AddCircle(val x: Float, val y: Float, val radius: Float, val dir: Path.Direction) : PathCommand()
    class AddOval(val l: Float, val t: Float, val r: Float, val b: Float, val dir: Path.Direction) : PathCommand()
    class AddRect(val l: Float, val t: Float, val r: Float, val b: Float, val dir: Path.Direction) : PathCommand()
    class AddRoundRect(val l: Float, val t: Float, val r: Float, val b: Float, val radii: FloatArray, val dir: Path.Direction) : PathCommand()
    class Transform(val values: FloatArray) : PathCommand()
    class AddPath(val path: Path) : PathCommand()
}

/** Offscreen ARGB image, like android.graphics.Bitmap. Create one with [Bitmap.createBitmap]. */
public abstract class Bitmap {
    public abstract val width: Int
    public abstract val height: Int

    /** Fills every pixel with [color]. */
    public abstract fun eraseColor(color: Int)

    /** Frees the pixels; the bitmap must not be drawn afterwards. */
    public abstract fun recycle()

    internal abstract fun newCanvas(): Canvas

    /**
     * Encodes the pixels as [format], like android.graphics.Bitmap.compress, and returns the file bytes.
     * [quality] from 0 to 100 applies to JPEG and lossy WEBP; PNG and lossless WEBP ignore it.
     */
    public abstract fun compress(format: CompressFormat, quality: Int): ByteArray

    public enum class Config { ARGB_8888 }

    public enum class CompressFormat { JPEG, PNG, WEBP_LOSSY, WEBP_LOSSLESS }

    public companion object {
        public fun createBitmap(width: Int, height: Int, config: Config): Bitmap = PlatformGraphics.createBitmap(width, height)
    }
}

/**
 * Drawing surface with the API of android.graphics.Canvas that the charts use. Implemented on top of
 * android.graphics.Canvas on Android and the Skia canvas on desktop and iOS.
 */
public open class Canvas {
    private var saves = 1

    public open val width: Int get() = 0
    public open val height: Int get() = 0

    public open fun save(): Int = saves++
    public open fun restore() {
        if (saves > 1) saves--
    }
    public open fun restoreToCount(saveCount: Int) {
        saves = saveCount.coerceAtLeast(1)
    }
    public open val saveCount: Int get() = saves

    public open fun translate(dx: Float, dy: Float) {}
    public open fun scale(sx: Float, sy: Float) {}
    public open fun rotate(degrees: Float) {}

    public fun rotate(degrees: Float, px: Float, py: Float) {
        translate(px, py)
        rotate(degrees)
        translate(-px, -py)
    }

    public open fun concat(matrix: Matrix) {}

    public open fun clipRect(left: Float, top: Float, right: Float, bottom: Float): Boolean = false
    public fun clipRect(rect: RectF): Boolean = clipRect(rect.left, rect.top, rect.right, rect.bottom)
    public fun clipRect(rect: Rect): Boolean = clipRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())
    public open fun clipPath(path: Path): Boolean = false

    public open fun drawColor(color: Int) {}
    public open fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {}

    public fun drawLines(pts: FloatArray, offset: Int, count: Int, paint: Paint) {
        var i = offset
        while (i + 3 < offset + count) {
            drawLine(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], paint)
            i += 4
        }
    }

    public fun drawLines(pts: FloatArray, paint: Paint): Unit = drawLines(pts, 0, pts.size, paint)

    public open fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {}
    public fun drawRect(rect: RectF, paint: Paint): Unit = drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
    public fun drawRect(rect: Rect, paint: Paint): Unit = drawRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), paint)
    public open fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, paint: Paint) {}
    public fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint): Unit = drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, paint)
    public open fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {}
    public open fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {}
    public fun drawOval(oval: RectF, paint: Paint): Unit = drawOval(oval.left, oval.top, oval.right, oval.bottom, paint)
    public open fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint) {}
    public fun drawArc(oval: RectF, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint): Unit =
        drawArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, useCenter, paint)
    public open fun drawPath(path: Path, paint: Paint) {}

    /** Draws [text] with its baseline at [y]; [x] is the left edge, center or right edge depending on [Paint.textAlign]. */
    public open fun drawText(text: String, x: Float, y: Float, paint: Paint) {}
    public fun drawText(text: CharSequence, start: Int, end: Int, x: Float, y: Float, paint: Paint): Unit =
        drawText(text.subSequence(start, end).toString(), x, y, paint)

    public open fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {}

    /** Retargets a canvas created from a bitmap. Null detaches it. */
    public open fun setBitmap(bitmap: Bitmap?) {}
}

/** Creates a canvas that draws into [bitmap], like `new Canvas(bitmap)`. */
public fun Canvas(bitmap: Bitmap): Canvas = bitmap.newCanvas()

/** Something that can be drawn in given bounds, like android.graphics.drawable.Drawable. */
public abstract class Drawable {
    private val boundsRect = Rect()

    public val bounds: Rect get() = boundsRect

    /** Natural width in pixels, or -1 when it has none. */
    public open val intrinsicWidth: Int get() = -1

    /** Natural height in pixels, or -1 when it has none. */
    public open val intrinsicHeight: Int get() = -1

    public open var alpha: Int = 255

    public var callback: Any? = null

    public fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        boundsRect.set(left, top, right, bottom)
    }

    public fun setBounds(bounds: Rect): Unit = setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)

    public fun copyBounds(bounds: Rect) {
        bounds.set(boundsRect)
    }

    public abstract fun draw(canvas: Canvas)
}

/** Text measurement backed by the platform font engine. */
internal expect object PlatformText {
    fun measureText(paint: Paint, text: String): Float
    fun getTextBounds(paint: Paint, text: String, bounds: Rect)
    fun getFontMetrics(paint: Paint, metrics: Paint.FontMetrics)

    /** The platform's own text layout, or null to use [SpanLayoutEngine]. */
    fun createLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
        spacingMult: Float,
        spacingAdd: Float,
        includePad: Boolean,
    ): TextLayoutEngine?
}

/** Platform factories. */
internal expect object PlatformGraphics {
    fun createBitmap(width: Int, height: Int): Bitmap
}

internal fun Float.roundToIntSafe(): Int = if (isNaN()) 0 else roundToInt()

/** Fills its bounds with one ARGB [color], like android.graphics.drawable.ColorDrawable. */
public class ColorDrawable(public var color: Int) : Drawable() {
    private val paint = Paint().apply { style = Paint.Style.FILL }

    override fun draw(canvas: Canvas) {
        paint.color = color
        val alphaScaled = (Color.alpha(color) * (alpha + (alpha shr 7))) shr 8
        if (alphaScaled == 0) return
        paint.alpha = alphaScaled
        canvas.drawRect(bounds, paint)
    }
}

/**
 * Rectangle filled with a linear gradient, like android.graphics.drawable.GradientDrawable with a linear gradient
 * shape. `<gradient android:angle="90" .../>` is [Orientation.BOTTOM_TOP].
 */
public class GradientDrawable(
    public val orientation: Orientation = Orientation.TOP_BOTTOM,
    public val colors: IntArray,
) : Drawable() {

    public enum class Orientation { TOP_BOTTOM, TR_BL, RIGHT_LEFT, BR_TL, BOTTOM_TOP, BL_TR, LEFT_RIGHT, TL_BR }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left.toFloat()
        val t = b.top.toFloat()
        val r = b.right.toFloat()
        val bt = b.bottom.toFloat()
        val (x0, y0, x1, y1) = when (orientation) {
            Orientation.TOP_BOTTOM -> floatArrayOf(l, t, l, bt)
            Orientation.TR_BL -> floatArrayOf(r, t, l, bt)
            Orientation.RIGHT_LEFT -> floatArrayOf(r, t, l, t)
            Orientation.BR_TL -> floatArrayOf(r, bt, l, t)
            Orientation.BOTTOM_TOP -> floatArrayOf(l, bt, l, t)
            Orientation.BL_TR -> floatArrayOf(l, bt, r, t)
            Orientation.LEFT_RIGHT -> floatArrayOf(l, t, r, t)
            Orientation.TL_BR -> floatArrayOf(l, t, r, bt)
        }
        paint.shader = LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP)
        // the shader gives the colors; the paint alpha is the drawable alpha, as in GradientDrawable
        paint.color = Color.argb(alpha, 0, 0, 0)
        canvas.drawRect(l, t, r, bt, paint)
    }
}

