package com.github.mikephil.charting.platform

import android.os.Build

/*
 * Android backend: every call goes to the real android.graphics classes, so the charts draw exactly what
 * MPAndroidChart draws.
 */

private const val HIDDEN_DEFAULT_FLAGS =
    android.graphics.Paint.DEV_KERN_TEXT_FLAG or android.graphics.Paint.EMBEDDED_BITMAP_TEXT_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG

internal fun Paint.toAndroid(): android.graphics.Paint {
    val cached = platformCache as? android.graphics.Paint
    if (cached != null && platformCacheVersion == version) return cached
    val p = cached ?: android.graphics.Paint()
    var f = HIDDEN_DEFAULT_FLAGS
    if (flags and Paint.ANTI_ALIAS_FLAG != 0) f = f or android.graphics.Paint.ANTI_ALIAS_FLAG
    if (flags and Paint.DITHER_FLAG != 0) f = f or android.graphics.Paint.DITHER_FLAG
    p.flags = f
    p.color = color
    p.style = when (style) {
        Paint.Style.FILL -> android.graphics.Paint.Style.FILL
        Paint.Style.STROKE -> android.graphics.Paint.Style.STROKE
        Paint.Style.FILL_AND_STROKE -> android.graphics.Paint.Style.FILL_AND_STROKE
    }
    p.strokeWidth = strokeWidth
    p.strokeCap = when (strokeCap) {
        Paint.Cap.BUTT -> android.graphics.Paint.Cap.BUTT
        Paint.Cap.ROUND -> android.graphics.Paint.Cap.ROUND
        Paint.Cap.SQUARE -> android.graphics.Paint.Cap.SQUARE
    }
    p.strokeJoin = when (strokeJoin) {
        Paint.Join.MITER -> android.graphics.Paint.Join.MITER
        Paint.Join.ROUND -> android.graphics.Paint.Join.ROUND
        Paint.Join.BEVEL -> android.graphics.Paint.Join.BEVEL
    }
    p.strokeMiter = strokeMiter
    p.textSize = textSize
    p.textAlign = when (textAlign) {
        Paint.Align.LEFT -> android.graphics.Paint.Align.LEFT
        Paint.Align.CENTER -> android.graphics.Paint.Align.CENTER
        Paint.Align.RIGHT -> android.graphics.Paint.Align.RIGHT
    }
    p.typeface = typeface?.toAndroid()
    p.pathEffect = (pathEffect as? DashPathEffect)?.let { android.graphics.DashPathEffect(it.intervals, it.phase) }
    p.shader = (shader as? LinearGradient)?.let {
        android.graphics.LinearGradient(it.x0, it.y0, it.x1, it.y1, it.colors, it.positions, it.tile.toAndroid())
    }
    p.xfermode = (xfermode as? PorterDuffXfermode)?.let { android.graphics.PorterDuffXfermode(it.mode.toAndroid()) }
    platformCache = p
    platformCacheVersion = version
    return p
}

private fun Shader.TileMode.toAndroid(): android.graphics.Shader.TileMode = when (this) {
    Shader.TileMode.CLAMP -> android.graphics.Shader.TileMode.CLAMP
    Shader.TileMode.REPEAT -> android.graphics.Shader.TileMode.REPEAT
    Shader.TileMode.MIRROR -> android.graphics.Shader.TileMode.MIRROR
    Shader.TileMode.DECAL -> if (Build.VERSION.SDK_INT >= 31) android.graphics.Shader.TileMode.DECAL else android.graphics.Shader.TileMode.CLAMP
}

private fun PorterDuff.Mode.toAndroid(): android.graphics.PorterDuff.Mode = android.graphics.PorterDuff.Mode.valueOf(name)

private val typefaceCache = HashMap<Typeface, android.graphics.Typeface>()

internal fun Typeface.toAndroid(): android.graphics.Typeface = typefaceCache.getOrPut(this) {
    val family = familyName
    val base = if (family == null) android.graphics.Typeface.DEFAULT else android.graphics.Typeface.create(family, android.graphics.Typeface.NORMAL)
    // a named weight such as sans-serif-medium is already in the family
    val namedWeight = family != null && family.startsWith("sans-serif-")
    when {
        !namedWeight && weight != 400 && weight != 700 && Build.VERSION.SDK_INT >= 28 ->
            android.graphics.Typeface.create(base, weight, isItalic)
        else -> android.graphics.Typeface.create(base, style)
    }
}

internal fun Matrix.toAndroid(): android.graphics.Matrix {
    val values = FloatArray(9)
    getValues(values)
    return android.graphics.Matrix().apply { setValues(values) }
}

private fun Path.Direction.toAndroid() = if (this == Path.Direction.CW) android.graphics.Path.Direction.CW else android.graphics.Path.Direction.CCW

internal fun Path.toAndroid(): android.graphics.Path {
    val cached = platformCache as? android.graphics.Path
    if (cached != null && platformCacheVersion == version) return cached
    val p = cached ?: android.graphics.Path()
    p.reset()
    p.fillType = android.graphics.Path.FillType.valueOf(fillType.name)
    for (c in commands) {
        when (c) {
            is PathCommand.MoveTo -> p.moveTo(c.x, c.y)
            is PathCommand.LineTo -> p.lineTo(c.x, c.y)
            is PathCommand.QuadTo -> p.quadTo(c.x1, c.y1, c.x2, c.y2)
            is PathCommand.CubicTo -> p.cubicTo(c.x1, c.y1, c.x2, c.y2, c.x3, c.y3)
            PathCommand.Close -> p.close()
            is PathCommand.ArcTo -> p.arcTo(android.graphics.RectF(c.l, c.t, c.r, c.b), c.start, c.sweep, c.forceMoveTo)
            is PathCommand.AddArc -> p.addArc(android.graphics.RectF(c.l, c.t, c.r, c.b), c.start, c.sweep)
            is PathCommand.AddCircle -> p.addCircle(c.x, c.y, c.radius, c.dir.toAndroid())
            is PathCommand.AddOval -> p.addOval(android.graphics.RectF(c.l, c.t, c.r, c.b), c.dir.toAndroid())
            is PathCommand.AddRect -> p.addRect(c.l, c.t, c.r, c.b, c.dir.toAndroid())
            is PathCommand.AddRoundRect -> p.addRoundRect(c.l, c.t, c.r, c.b, c.radii, c.dir.toAndroid())
            is PathCommand.Transform -> p.transform(android.graphics.Matrix().apply { setValues(c.values) })
            is PathCommand.AddPath -> p.addPath(c.path.toAndroid())
        }
    }
    platformCache = p
    platformCacheVersion = version
    return p
}

/** [Canvas] drawing into an android.graphics.Canvas. */
public class AndroidCanvas(
    public val native: android.graphics.Canvas,
    override val width: Int = native.width,
    override val height: Int = native.height,
) : Canvas() {
    override fun save(): Int = native.save()
    override fun restore(): Unit = native.restore()
    override fun restoreToCount(saveCount: Int): Unit = native.restoreToCount(saveCount)
    override val saveCount: Int get() = native.saveCount
    override fun translate(dx: Float, dy: Float): Unit = native.translate(dx, dy)
    override fun scale(sx: Float, sy: Float): Unit = native.scale(sx, sy)
    override fun rotate(degrees: Float): Unit = native.rotate(degrees)
    override fun concat(matrix: Matrix): Unit = native.concat(matrix.toAndroid())
    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float): Boolean = native.clipRect(left, top, right, bottom)
    override fun clipPath(path: Path): Boolean = native.clipPath(path.toAndroid())
    override fun drawColor(color: Int): Unit = native.drawColor(color)
    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint): Unit =
        native.drawLine(startX, startY, stopX, stopY, paint.toAndroid())
    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint): Unit =
        native.drawRect(left, top, right, bottom, paint.toAndroid())
    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, paint: Paint): Unit =
        native.drawRoundRect(left, top, right, bottom, rx, ry, paint.toAndroid())
    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint): Unit = native.drawCircle(cx, cy, radius, paint.toAndroid())
    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint): Unit =
        native.drawOval(left, top, right, bottom, paint.toAndroid())
    override fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint): Unit =
        native.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint.toAndroid())
    override fun drawPath(path: Path, paint: Paint): Unit = native.drawPath(path.toAndroid(), paint.toAndroid())
    override fun drawText(text: String, x: Float, y: Float, paint: Paint): Unit = native.drawText(text, x, y, paint.toAndroid())
    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?): Unit =
        native.drawBitmap((bitmap as AndroidBitmap).native, left, top, paint?.toAndroid())
    override fun setBitmap(bitmap: Bitmap?): Unit = native.setBitmap((bitmap as AndroidBitmap?)?.native)
}

/** [Bitmap] backed by an android.graphics.Bitmap. */
public class AndroidBitmap(public val native: android.graphics.Bitmap) : Bitmap() {
    override val width: Int get() = native.width
    override val height: Int get() = native.height
    override fun eraseColor(color: Int): Unit = native.eraseColor(color)
    override fun recycle(): Unit = native.recycle()
    override fun newCanvas(): Canvas = AndroidCanvas(android.graphics.Canvas(native))

    override fun compress(format: CompressFormat, quality: Int): ByteArray {
        val nativeFormat = when (format) {
            CompressFormat.JPEG -> android.graphics.Bitmap.CompressFormat.JPEG
            CompressFormat.PNG -> android.graphics.Bitmap.CompressFormat.PNG
            CompressFormat.WEBP_LOSSY -> if (Build.VERSION.SDK_INT >= 30) android.graphics.Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") android.graphics.Bitmap.CompressFormat.WEBP
            CompressFormat.WEBP_LOSSLESS -> if (Build.VERSION.SDK_INT >= 30) android.graphics.Bitmap.CompressFormat.WEBP_LOSSLESS else @Suppress("DEPRECATION") android.graphics.Bitmap.CompressFormat.WEBP
        }
        val out = java.io.ByteArrayOutputStream()
        native.compress(nativeFormat, quality, out)
        return out.toByteArray()
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
    ): TextLayoutEngine? = AndroidTextLayout(text, paint, width, alignment, spacingMult, spacingAdd, includePad)

    private val rect = android.graphics.Rect()
    private val metrics = android.graphics.Paint.FontMetrics()

    actual fun measureText(paint: Paint, text: String): Float = paint.toAndroid().measureText(text)

    actual fun getTextBounds(paint: Paint, text: String, bounds: Rect) {
        paint.toAndroid().getTextBounds(text, 0, text.length, rect)
        bounds.set(rect.left, rect.top, rect.right, rect.bottom)
    }

    actual fun getFontMetrics(paint: Paint, metrics: Paint.FontMetrics) {
        val m = this.metrics
        paint.toAndroid().getFontMetrics(m)
        metrics.top = m.top
        metrics.ascent = m.ascent
        metrics.descent = m.descent
        metrics.bottom = m.bottom
        metrics.leading = m.leading
    }
}

internal actual object PlatformGraphics {
    actual fun createBitmap(width: Int, height: Int): Bitmap =
        AndroidBitmap(android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888))
}

/**
 * The real android.text.StaticLayout. Its TextPaint takes the state of the chart's paint again before every draw,
 * as Android's layout reads its paint when drawing.
 */
private class AndroidTextLayout(
    text: CharSequence,
    private val paint: TextPaint,
    width: Int,
    alignment: Layout.Alignment,
    spacingMult: Float,
    spacingAdd: Float,
    includePad: Boolean,
) : TextLayoutEngine {
    private val nativePaint = android.text.TextPaint().apply { set(paint.toAndroid()) }

    private val layout: android.text.StaticLayout = run {
        val nativeText = text.toAndroidText()
        val nativeAlignment = when (alignment) {
            Layout.Alignment.ALIGN_NORMAL -> android.text.Layout.Alignment.ALIGN_NORMAL
            Layout.Alignment.ALIGN_OPPOSITE -> android.text.Layout.Alignment.ALIGN_OPPOSITE
            Layout.Alignment.ALIGN_CENTER -> android.text.Layout.Alignment.ALIGN_CENTER
        }
        android.text.StaticLayout.Builder.obtain(nativeText, 0, nativeText.length, nativePaint, width)
            .setAlignment(nativeAlignment)
            .setLineSpacing(spacingAdd, spacingMult)
            .setIncludePad(includePad)
            .build()
    }

    override val lineCount: Int get() = layout.lineCount
    override val height: Int get() = layout.height
    override fun getLineWidth(line: Int): Float = layout.getLineWidth(line)

    override fun draw(canvas: Canvas) {
        val native = (canvas as? AndroidCanvas)?.native ?: return
        nativePaint.set(paint.toAndroid())
        layout.draw(native)
    }
}

/** Converts a [Spanned] to an android.text.SpannableString with the matching android.text.style spans. */
private fun CharSequence.toAndroidText(): CharSequence {
    val spanned = this as? Spanned ?: return toString()
    val result = android.text.SpannableString(toString())
    for (span in spanned.getSpans(0, length, Any::class)) {
        val native: Any = when (span) {
            is RelativeSizeSpan -> android.text.style.RelativeSizeSpan(span.sizeChange)
            is AbsoluteSizeSpan -> android.text.style.AbsoluteSizeSpan(span.size, span.dip)
            is ForegroundColorSpan -> android.text.style.ForegroundColorSpan(span.foregroundColor)
            is StyleSpan -> android.text.style.StyleSpan(span.style)
            is TypefaceSpan -> android.text.style.TypefaceSpan(span.family)
            else -> continue
        }
        result.setSpan(native, spanned.getSpanStart(span), spanned.getSpanEnd(span), spanned.getSpanFlags(span))
    }
    return result
}
