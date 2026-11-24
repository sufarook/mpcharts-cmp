package com.github.mikephil.charting.platform

import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Data
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontEdging
import org.jetbrains.skia.FontHinting
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontSlant
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.FontVariation
import org.jetbrains.skia.Gradient
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathDirection
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.TextLine
import kotlin.math.ceil
import kotlin.math.floor

/*
 * Skia backend for desktop and iOS. android.graphics is a thin layer over Skia, so these calls are the ones
 * Android makes for the same chart code. Text uses the same Roboto that Android ships, loaded by the chart
 * composables, and is shaped with HarfBuzz like Android's text stack.
 */

/** Fonts used for chart text on Skia platforms. */
public object SkiaFonts {
    private var roboto: org.jetbrains.skia.Typeface? = null
    private var robotoItalic: org.jetbrains.skia.Typeface? = null
    private val cache = HashMap<Typeface?, org.jetbrains.skia.Typeface>()

    public val isLoaded: Boolean get() = roboto != null

    /** Installs the variable Roboto font, the default font of Android. */
    public fun loadRoboto(bytes: ByteArray) {
        if (roboto != null) return
        roboto = FontMgr.default.makeFromData(Data.makeFromBytes(bytes), 0)
        cache.clear()
        fontCache.clear()
    }

    /** Installs the variable Roboto Italic font, which Android uses for italic sans-serif text. */
    public fun loadRobotoItalic(bytes: ByteArray) {
        if (robotoItalic != null) return
        robotoItalic = FontMgr.default.makeFromData(Data.makeFromBytes(bytes), 0)
        cache.clear()
        fontCache.clear()
    }

    /** Whether [typeface] is drawn with a real italic font, so no slant has to be faked. */
    internal fun hasRealItalic(typeface: Typeface?): Boolean {
        val family = typeface?.familyName
        return typeface?.isItalic == true && robotoItalic != null && (family == null || family.startsWith("sans-serif"))
    }

    internal fun typeface(typeface: Typeface?): org.jetbrains.skia.Typeface = cache.getOrPut(typeface) {
        val family = typeface?.familyName
        val weight = typeface?.weight ?: 400
        val italic = typeface?.isItalic ?: false
        val isSans = family == null || family.startsWith("sans-serif")
        val roboto = if (italic) robotoItalic ?: roboto else roboto
        if (isSans && roboto != null) {
            val axes = roboto.variationAxes?.map { it.tag }.orEmpty()
            val variations = buildList {
                if ("wght" in axes) add(FontVariation("wght", weight.toFloat()))
                if (italic && "ital" in axes) add(FontVariation("ital", 1f))
            }
            if (variations.isEmpty()) roboto else roboto.makeClone(variations.toTypedArray(), 0)
        } else {
            val name = when (family) {
                null, "sans-serif" -> ""
                "serif" -> "serif"
                "monospace" -> "monospace"
                else -> family
            }
            val style = FontStyle(weight, 5, if (italic) FontSlant.ITALIC else FontSlant.UPRIGHT)
            FontMgr.default.matchFamilyStyle(name, style) ?: FontMgr.default.legacyMakeTypeface(name, style)
                ?: FontMgr.default.legacyMakeTypeface("", FontStyle.NORMAL)!!
        }
    }
}

private data class FontKey(val typeface: Typeface?, val size: Float, val antiAlias: Boolean)

private val fontCache = HashMap<FontKey, Font>()

internal fun Paint.skiaFont(): Font = fontCache.getOrPut(FontKey(typeface, textSize, isAntiAlias)) { makeFont() }

private fun Paint.makeFont(): Font {
    val font = Font(SkiaFonts.typeface(typeface), textSize)
    font.edging = if (isAntiAlias) FontEdging.ANTI_ALIAS else FontEdging.ALIAS
    font.hinting = FontHinting.NORMAL
    font.isSubpixel = true
    val italic = typeface?.isItalic == true
    val hasItalicAxis = SkiaFonts.typeface(typeface).variationAxes?.any { it.tag == "ital" } == true
    // like Android, fake an italic the font does not have by skewing
    if (italic && !hasItalicAxis && !SkiaFonts.hasRealItalic(typeface)) font.skewX = -0.25f
    return font
}

private class SkiaPaintCache(val paint: org.jetbrains.skia.Paint, val font: Font)

private fun Paint.skiaCache(): SkiaPaintCache {
    val cached = platformCache as? SkiaPaintCache
    if (cached != null && platformCacheVersion == version) return cached
    val p = cached?.paint ?: org.jetbrains.skia.Paint()
    p.isAntiAlias = isAntiAlias
    p.color = color
    p.mode = when (style) {
        Paint.Style.FILL -> PaintMode.FILL
        Paint.Style.STROKE -> PaintMode.STROKE
        Paint.Style.FILL_AND_STROKE -> PaintMode.STROKE_AND_FILL
    }
    p.strokeWidth = strokeWidth
    p.strokeCap = when (strokeCap) {
        Paint.Cap.BUTT -> PaintStrokeCap.BUTT
        Paint.Cap.ROUND -> PaintStrokeCap.ROUND
        Paint.Cap.SQUARE -> PaintStrokeCap.SQUARE
    }
    p.strokeJoin = when (strokeJoin) {
        Paint.Join.MITER -> PaintStrokeJoin.MITER
        Paint.Join.ROUND -> PaintStrokeJoin.ROUND
        Paint.Join.BEVEL -> PaintStrokeJoin.BEVEL
    }
    p.strokeMiter = strokeMiter
    p.pathEffect = (pathEffect as? DashPathEffect)?.let { org.jetbrains.skia.PathEffect.makeDash(it.intervals, it.phase) }
    p.shader = (shader as? LinearGradient)?.let { g ->
        val tile = when (g.tile) {
            Shader.TileMode.CLAMP -> FilterTileMode.CLAMP
            Shader.TileMode.REPEAT -> FilterTileMode.REPEAT
            Shader.TileMode.MIRROR -> FilterTileMode.MIRROR
            Shader.TileMode.DECAL -> FilterTileMode.DECAL
        }
        val colors = Array(g.colors.size) { Color4f(g.colors[it]) }
        org.jetbrains.skia.Shader.makeLinearGradient(g.x0, g.y0, g.x1, g.y1, Gradient(Gradient.Colors(colors, g.positions, tile), Gradient.Interpolation()))
    }
    p.blendMode = when ((xfermode as? PorterDuffXfermode)?.mode) {
        null, PorterDuff.Mode.SRC_OVER -> BlendMode.SRC_OVER
        PorterDuff.Mode.CLEAR -> BlendMode.CLEAR
        PorterDuff.Mode.SRC -> BlendMode.SRC
        PorterDuff.Mode.DST -> BlendMode.DST
        PorterDuff.Mode.DST_OVER -> BlendMode.DST_OVER
        PorterDuff.Mode.SRC_IN -> BlendMode.SRC_IN
        PorterDuff.Mode.DST_IN -> BlendMode.DST_IN
        PorterDuff.Mode.SRC_OUT -> BlendMode.SRC_OUT
        PorterDuff.Mode.DST_OUT -> BlendMode.DST_OUT
        PorterDuff.Mode.SRC_ATOP -> BlendMode.SRC_ATOP
        PorterDuff.Mode.DST_ATOP -> BlendMode.DST_ATOP
        PorterDuff.Mode.XOR -> BlendMode.XOR
    }
    val cache = SkiaPaintCache(p, skiaFont())
    platformCache = cache
    platformCacheVersion = version
    return cache
}

internal fun Paint.toSkia(): org.jetbrains.skia.Paint = skiaCache().paint

private fun Path.Direction.toSkia() = if (this == Path.Direction.CW) PathDirection.CLOCKWISE else PathDirection.COUNTER_CLOCKWISE

internal fun Path.toSkia(): org.jetbrains.skia.Path {
    val cached = platformCache as? org.jetbrains.skia.Path
    if (cached != null && platformCacheVersion == version) return cached
    val b = PathBuilder(
        when (fillType) {
            Path.FillType.WINDING -> PathFillMode.WINDING
            Path.FillType.EVEN_ODD -> PathFillMode.EVEN_ODD
            Path.FillType.INVERSE_WINDING -> PathFillMode.INVERSE_WINDING
            Path.FillType.INVERSE_EVEN_ODD -> PathFillMode.INVERSE_EVEN_ODD
        },
    )
    for (c in commands) {
        when (c) {
            is PathCommand.MoveTo -> b.moveTo(c.x, c.y)
            is PathCommand.LineTo -> b.lineTo(c.x, c.y)
            is PathCommand.QuadTo -> b.quadTo(c.x1, c.y1, c.x2, c.y2)
            is PathCommand.CubicTo -> b.cubicTo(c.x1, c.y1, c.x2, c.y2, c.x3, c.y3)
            PathCommand.Close -> b.closePath()
            is PathCommand.ArcTo -> b.arcTo(c.l, c.t, c.r, c.b, c.start, c.sweep, c.forceMoveTo)
            is PathCommand.AddArc -> b.addArc(c.l, c.t, c.r, c.b, c.start, c.sweep)
            is PathCommand.AddCircle -> b.addCircle(c.x, c.y, c.radius, c.dir.toSkia())
            // Android's addOval and addRoundRect use Skia's default start points
            is PathCommand.AddOval -> b.addOval(c.l, c.t, c.r, c.b, c.dir.toSkia(), 1)
            is PathCommand.AddRect -> b.addRect(c.l, c.t, c.r, c.b, c.dir.toSkia(), 0)
            is PathCommand.AddRoundRect -> b.addRRect(
                RRect.makeComplexLTRB(c.l, c.t, c.r, c.b, c.radii),
                c.dir.toSkia(),
                if (c.dir == Path.Direction.CW) 6 else 7,
            )
            is PathCommand.Transform -> b.transform(c.values.toMatrix33())
            is PathCommand.AddPath -> b.addPath(c.path.toSkia(), org.jetbrains.skia.PathAddMode.APPEND)
        }
    }
    val path = b.detach()
    b.close()
    platformCache = path
    platformCacheVersion = version
    return path
}

/** [Canvas] drawing into a Skia canvas. */
public class SkiaCanvas(
    public var native: org.jetbrains.skia.Canvas?,
    override val width: Int,
    override val height: Int,
) : Canvas() {
    private val c: org.jetbrains.skia.Canvas get() = native ?: error("Canvas has no bitmap")

    override fun save(): Int = c.save()
    override fun restore() {
        c.restore()
    }
    override fun restoreToCount(saveCount: Int) {
        c.restoreToCount(saveCount)
    }
    override val saveCount: Int get() = c.saveCount
    override fun translate(dx: Float, dy: Float) {
        c.translate(dx, dy)
    }
    override fun scale(sx: Float, sy: Float) {
        c.scale(sx, sy)
    }
    override fun rotate(degrees: Float) {
        c.rotate(degrees)
    }
    override fun concat(matrix: Matrix) {
        val values = FloatArray(9)
        matrix.getValues(values)
        c.concat(values.toMatrix33())
    }

    // Android clips rectangles without and paths with antialiasing
    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        c.clipRect(left, top, right, bottom, ClipMode.INTERSECT, false)
        return true
    }

    override fun clipPath(path: Path): Boolean {
        c.clipPath(path.toSkia(), ClipMode.INTERSECT, true)
        return true
    }

    private val colorPaint = org.jetbrains.skia.Paint()

    override fun drawColor(color: Int) {
        colorPaint.color = color
        colorPaint.blendMode = BlendMode.SRC_OVER
        c.drawPaint(colorPaint)
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        c.drawLine(startX, startY, stopX, stopY, paint.toSkia())
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        c.drawRect(org.jetbrains.skia.Rect.makeLTRB(left, top, right, bottom), paint.toSkia())
    }

    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, paint: Paint) {
        c.drawRRect(RRect.makeLTRB(left, top, right, bottom, rx, ry), paint.toSkia())
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        c.drawCircle(cx, cy, radius, paint.toSkia())
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        c.drawOval(left, top, right, bottom, paint.toSkia())
    }

    override fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint) {
        c.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint.toSkia())
    }

    override fun drawPath(path: Path, paint: Paint) {
        c.drawPath(path.toSkia(), paint.toSkia())
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        if (text.isEmpty()) return
        val cache = paint.skiaCache()
        val line = TextLines.get(text, cache.font)
        val left = when (paint.textAlign) {
            Paint.Align.LEFT -> x
            Paint.Align.CENTER -> x - line.width / 2f
            Paint.Align.RIGHT -> x - line.width
        }
        c.drawTextLine(line, left, y, cache.paint)
    }

    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
        val image = (bitmap as SkiaBitmap).surface.makeImageSnapshot()
        if (paint == null) c.drawImage(image, left, top) else c.drawImage(image, left, top, paint.toSkia())
        image.close()
    }

    override fun setBitmap(bitmap: Bitmap?) {
        native = (bitmap as SkiaBitmap?)?.surface?.canvas
    }
}

/** [Bitmap] backed by a Skia raster surface. */
public class SkiaBitmap(override val width: Int, override val height: Int) : Bitmap() {
    internal val surface: Surface = Surface.makeRasterN32Premul(width, height)

    override fun eraseColor(color: Int) {
        surface.canvas.clear(color)
    }

    override fun recycle() {
        surface.close()
    }

    override fun newCanvas(): Canvas = SkiaCanvas(surface.canvas, width, height)

    override fun compress(format: CompressFormat, quality: Int): ByteArray {
        val (skiaFormat, skiaQuality) = when (format) {
            CompressFormat.JPEG -> org.jetbrains.skia.EncodedImageFormat.JPEG to quality
            CompressFormat.PNG -> org.jetbrains.skia.EncodedImageFormat.PNG to 100
            CompressFormat.WEBP_LOSSY -> org.jetbrains.skia.EncodedImageFormat.WEBP to quality
            // Skia's WEBP encoder is lossless at quality 100
            CompressFormat.WEBP_LOSSLESS -> org.jetbrains.skia.EncodedImageFormat.WEBP to 100
        }
        val image = surface.makeImageSnapshot()
        try {
            return image.encodeToData(skiaFormat, skiaQuality)?.bytes ?: ByteArray(0)
        } finally {
            image.close()
        }
    }
}

/** Small cache of shaped text lines; chart labels repeat from frame to frame. */
internal object TextLines {
    private const val MAX = 512
    private val cache = LinkedHashMap<Pair<String, Font>, TextLine>()

    fun get(text: String, font: Font): TextLine {
        val key = text to font
        cache[key]?.let { return it }
        val line = TextLine.make(text, font)
        if (cache.size >= MAX) {
            val oldest = cache.keys.first()
            cache.remove(oldest)?.close()
        }
        cache[key] = line
        return line
    }
}

internal actual object PlatformText {
    actual fun createLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
        spacingMult: Float,
        spacingAdd: Float,
        includePad: Boolean,
    ): TextLayoutEngine? = null

    actual fun measureText(paint: Paint, text: String): Float = TextLines.get(text, paint.skiaCache().font).width

    actual fun getTextBounds(paint: Paint, text: String, bounds: Rect) {
        val cache = paint.skiaCache()
        val r = cache.font.measureText(text, cache.paint)
        // Android rounds the glyph bounds out to whole pixels
        bounds.set(floor(r.left).toInt(), floor(r.top).toInt(), ceil(r.right).toInt(), ceil(r.bottom).toInt())
    }

    actual fun getFontMetrics(paint: Paint, metrics: Paint.FontMetrics) {
        val font = paint.skiaCache().font
        val units = FontUnits.of(font.typeface)
        if (units == null) {
            val m = font.metrics
            metrics.top = m.top
            metrics.ascent = m.ascent
            metrics.descent = m.descent
            metrics.bottom = m.bottom
            metrics.leading = m.leading
            return
        }
        val scale = font.size / units.unitsPerEm
        metrics.top = -units.yMax * scale
        metrics.ascent = -units.ascender * scale
        metrics.descent = -units.descender * scale
        metrics.bottom = -units.yMin * scale
        metrics.leading = units.lineGap * scale
    }
}

/**
 * Vertical font metrics read from the font tables the way Skia's FreeType backend reads them, which is what
 * Android reports. Skia's DirectWrite (Windows) and CoreText (Apple) backends report other values for the
 * same font, which would move every label.
 */
internal class FontUnits(
    val unitsPerEm: Float,
    val ascender: Float,
    val descender: Float,
    val lineGap: Float,
    val yMin: Float,
    val yMax: Float,
) {
    companion object {
        private val cache = HashMap<Int, FontUnits?>()

        fun of(typeface: org.jetbrains.skia.Typeface?): FontUnits? {
            typeface ?: return null
            return cache.getOrPut(typeface.uniqueId) { read(typeface) }
        }

        private fun read(typeface: org.jetbrains.skia.Typeface): FontUnits? {
            val head = typeface.getTableData("head")?.bytes ?: return null
            val hhea = typeface.getTableData("hhea")?.bytes ?: return null
            val os2 = typeface.getTableData("OS/2")?.bytes
            val upem = u16(head, 18).toFloat()
            if (upem <= 0f) return null
            val yMin = s16(head, 38).toFloat()
            val yMax = s16(head, 42).toFloat()
            // FreeType: hhea, or the OS/2 typo values when hhea has none
            var ascender = s16(hhea, 4).toFloat()
            var descender = s16(hhea, 6).toFloat()
            var height = ascender - descender + s16(hhea, 8)
            if (ascender == 0f && descender == 0f && os2 != null && os2.size >= 74) {
                ascender = s16(os2, 68).toFloat()
                descender = s16(os2, 70).toFloat()
                height = ascender - descender + s16(os2, 72)
            }
            var lineGap = height - ascender + descender
            // Skia: the OS/2 typo values win when the font asks for them with USE_TYPO_METRICS
            if (os2 != null && os2.size >= 74 && u16(os2, 0) != 0xFFFF && (u16(os2, 62) and (1 shl 7)) != 0) {
                ascender = s16(os2, 68).toFloat()
                descender = s16(os2, 70).toFloat()
                lineGap = s16(os2, 72).toFloat()
            }
            return FontUnits(upem, ascender, descender, lineGap, yMin, yMax)
        }

        private fun u16(b: ByteArray, o: Int): Int = ((b[o].toInt() and 0xFF) shl 8) or (b[o + 1].toInt() and 0xFF)
        private fun s16(b: ByteArray, o: Int): Int = u16(b, o).toShort().toInt()
    }
}

internal actual object PlatformGraphics {
    actual fun createBitmap(width: Int, height: Int): Bitmap = SkiaBitmap(width, height)
}

private fun FloatArray.toMatrix33(): Matrix33 = Matrix33(this[0], this[1], this[2], this[3], this[4], this[5], this[6], this[7], this[8])
