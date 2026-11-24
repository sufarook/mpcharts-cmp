package com.github.mikephil.charting.platform

/** Text layout base, like android.text.Layout. */
public abstract class Layout internal constructor(
    public val text: CharSequence,
    public val paint: TextPaint,
    public val width: Int,
    public val alignment: Alignment,
) {
    public enum class Alignment { ALIGN_NORMAL, ALIGN_OPPOSITE, ALIGN_CENTER }

    public abstract val lineCount: Int
    public abstract val height: Int
    public abstract fun getLineWidth(line: Int): Float
    public abstract fun draw(canvas: Canvas)
}

/**
 * Multi-line text wrapped to a width, like android.text.StaticLayout built with its builder. [text] may be a
 * [Spanned], such as a [SpannableString], whose size, color and style spans change parts of the text.
 *
 * On Android this is the real android.text.StaticLayout. On desktop and iOS the text is broken greedily at spaces
 * (no hyphenation), each line is as tall as the largest font on it, and the paint is read again when drawing, as
 * Android does.
 */
public class StaticLayout private constructor(
    text: CharSequence,
    paint: TextPaint,
    width: Int,
    alignment: Alignment,
    spacingMult: Float,
    spacingAdd: Float,
    includePad: Boolean,
) : Layout(text, paint, width, alignment) {

    private val engine: TextLayoutEngine =
        PlatformText.createLayout(text, paint, width, alignment, spacingMult, spacingAdd, includePad)
            ?: SpanLayoutEngine(text, paint, width, alignment, spacingMult, spacingAdd, includePad)

    override val lineCount: Int get() = engine.lineCount

    override val height: Int get() = engine.height

    override fun getLineWidth(line: Int): Float = engine.getLineWidth(line)

    override fun draw(canvas: Canvas): Unit = engine.draw(canvas)

    /** Builder with the defaults of StaticLayout.Builder. */
    public class Builder private constructor(
        private val text: CharSequence,
        private val start: Int,
        private val end: Int,
        private val paint: TextPaint,
        private val width: Int,
    ) {
        private var alignment = Alignment.ALIGN_NORMAL
        private var spacingAdd = 0f
        private var spacingMult = 1f
        private var includePad = true

        public fun setAlignment(alignment: Alignment): Builder = apply { this.alignment = alignment }

        public fun setLineSpacing(spacingAdd: Float, spacingMult: Float): Builder = apply {
            this.spacingAdd = spacingAdd
            this.spacingMult = spacingMult
        }

        public fun setIncludePad(includePad: Boolean): Builder = apply { this.includePad = includePad }

        public fun build(): StaticLayout =
            StaticLayout(text.subSequence(start, end), paint, width, alignment, spacingMult, spacingAdd, includePad)

        public companion object {
            public fun obtain(text: CharSequence, start: Int, end: Int, paint: TextPaint, width: Int): Builder =
                Builder(text, start, end, paint, width)
        }
    }
}

/** A laid out text: the real StaticLayout on Android, [SpanLayoutEngine] elsewhere. */
internal interface TextLayoutEngine {
    val lineCount: Int
    val height: Int
    fun getLineWidth(line: Int): Float
    fun draw(canvas: Canvas)
}

/**
 * Lays out styled text like StaticLayout: greedy line breaking at spaces, words too long for a line break by
 * character, lines as tall as the largest font on them, `includePad` using the font's top on the first line and
 * its bottom on the last, extra line spacing on every line but the last.
 */
internal class SpanLayoutEngine(
    private val text: CharSequence,
    private val paint: TextPaint,
    private val width: Int,
    private val alignment: Layout.Alignment,
    private val spacingMult: Float,
    private val spacingAdd: Float,
    private val includePad: Boolean,
) : TextLayoutEngine {

    private val string = text.toString()

    /** Ranges with the same spans, each with the spans that cover it in the order they were set. */
    private class Run(val start: Int, val end: Int, val styles: List<CharacterStyle>)

    private val runs: List<Run> = buildRuns()

    private var runPaints: List<TextPaint> = emptyList()
    private var runPaintsVersion = -1

    /** Start, end without the trailing spaces and line break, and top, baseline and bottom of each line. */
    private class Line(val start: Int, val visibleEnd: Int, val end: Int) {
        var top = 0f
        var baseline = 0f
        var bottom = 0f
    }

    private val lines: List<Line>

    init {
        val paints = paints()
        lines = breakLines(paints)
        var top = 0f
        for ((i, line) in lines.withIndex()) {
            var ascent = 0f
            var descent = 0f
            var fontTop = 0f
            var fontBottom = 0f
            var first = true
            val metricsEnd = maxOf(line.end, line.start + 1).coerceAtMost(string.length)
            for ((r, run) in runs.withIndex()) {
                if (run.end <= line.start || run.start >= metricsEnd) continue
                val m = paints[r].fontMetrics
                if (first || m.ascent < ascent) ascent = m.ascent
                if (first || m.descent > descent) descent = m.descent
                if (first || m.top < fontTop) fontTop = m.top
                if (first || m.bottom > fontBottom) fontBottom = m.bottom
                first = false
            }
            if (first) {
                val m = paint.fontMetrics
                ascent = m.ascent
                descent = m.descent
                fontTop = m.top
                fontBottom = m.bottom
            }
            val above = if (includePad && i == 0) fontTop else ascent
            val below = if (includePad && i == lines.lastIndex) fontBottom else descent
            var height = below - above
            if (i != lines.lastIndex) height = height * spacingMult + spacingAdd
            line.top = top
            line.baseline = top - above
            line.bottom = top + height
            top += height
        }
    }

    override val lineCount: Int get() = lines.size

    override val height: Int get() = ((lines.lastOrNull()?.bottom ?: 0f) + 0.5f).toInt()

    override fun getLineWidth(line: Int): Float = lines[line].let { measure(it.start, it.visibleEnd, paints()) }

    override fun draw(canvas: Canvas) {
        val paints = paints()
        for (line in lines) {
            if (line.visibleEnd <= line.start) continue
            val lineWidth = measure(line.start, line.visibleEnd, paints)
            // Layout.drawText works with the integer line extent
            val extent = kotlin.math.ceil(lineWidth).toInt()
            var x = when (alignment) {
                Layout.Alignment.ALIGN_NORMAL -> 0f
                Layout.Alignment.ALIGN_CENTER -> ((width - extent) shr 1).toFloat()
                Layout.Alignment.ALIGN_OPPOSITE -> (width - extent).toFloat()
            }
            for ((r, run) in runs.withIndex()) {
                val start = maxOf(run.start, line.start)
                val end = minOf(run.end, line.visibleEnd)
                if (start >= end) continue
                val piece = string.substring(start, end)
                canvas.drawText(piece, x, line.baseline, paints[r])
                x += paints[r].measureText(piece)
            }
        }
    }

    /** One paint per run: the layout's paint as it is now with the run's spans applied, as Android draws. */
    private fun paints(): List<TextPaint> {
        if (runPaintsVersion == paint.version && runPaints.size == runs.size) return runPaints
        runPaints = runs.map { run ->
            TextPaint().apply {
                set(paint)
                for (style in run.styles) style.updateDrawState(this)
                textAlign = Paint.Align.LEFT
            }
        }
        runPaintsVersion = paint.version
        return runPaints
    }

    private fun buildRuns(): List<Run> {
        val length = string.length
        val spanned = text as? Spanned
        if (spanned == null || length == 0) return listOf(Run(0, length, emptyList()))
        val spans = spanned.getSpans(0, length, CharacterStyle::class)
        val bounds = mutableSetOf(0, length)
        for (span in spans) {
            bounds.add(spanned.getSpanStart(span).coerceIn(0, length))
            bounds.add(spanned.getSpanEnd(span).coerceIn(0, length))
        }
        val points = bounds.sorted()
        return (0 until points.size - 1).map { i ->
            val start = points[i]
            val end = points[i + 1]
            Run(start, end, spans.filter { spanned.getSpanStart(it) <= start && spanned.getSpanEnd(it) >= end })
        }
    }

    private fun measure(start: Int, end: Int, paints: List<TextPaint>): Float {
        var width = 0f
        for ((r, run) in runs.withIndex()) {
            val s = maxOf(run.start, start)
            val e = minOf(run.end, end)
            if (s < e) width += paints[r].measureText(string.substring(s, e))
        }
        return width
    }

    private fun trimEnd(start: Int, end: Int): Int {
        var e = end
        while (e > start && string[e - 1] == ' ') e--
        return e
    }

    /** Greedy line breaking: words go on the current line while they fit; too long words break by character. */
    private fun breakLines(paints: List<TextPaint>): List<Line> {
        val result = mutableListOf<Line>()
        val max = width.toFloat()
        fun fits(start: Int, end: Int) = measure(start, trimEnd(start, end), paints) <= max
        fun add(start: Int, end: Int, lineEnd: Int) = result.add(Line(start, trimEnd(start, end), lineEnd))

        var paragraphStart = 0
        while (paragraphStart <= string.length) {
            var paragraphEnd = string.indexOf('\n', paragraphStart)
            if (paragraphEnd < 0) paragraphEnd = string.length
            // a line keeps its line break, so its metrics include it as on Android
            val breakEnd = if (paragraphEnd < string.length) paragraphEnd + 1 else paragraphEnd

            if (paragraphStart == paragraphEnd) {
                result.add(Line(paragraphStart, paragraphStart, breakEnd))
            } else {
                var lineStart = paragraphStart
                var lineEnd = paragraphStart
                var i = paragraphStart
                while (i < paragraphEnd) {
                    // the next word with its trailing spaces
                    var j = i
                    while (j < paragraphEnd && string[j] != ' ') j++
                    while (j < paragraphEnd && string[j] == ' ') j++
                    if (lineEnd == lineStart || fits(lineStart, j)) {
                        if (lineEnd == lineStart && !fits(i, j)) {
                            // the word alone does not fit: break it by character
                            var chunkStart = i
                            for (k in i until j) {
                                if (k > chunkStart && !fits(chunkStart, k + 1)) {
                                    add(chunkStart, k, k)
                                    chunkStart = k
                                }
                            }
                            lineStart = chunkStart
                        }
                        lineEnd = j
                    } else {
                        add(lineStart, lineEnd, lineEnd)
                        lineStart = i
                        lineEnd = j
                    }
                    i = j
                }
                add(lineStart, lineEnd, breakEnd)
            }
            if (paragraphEnd >= string.length) break
            paragraphStart = paragraphEnd + 1
        }
        return result
    }
}

/** Text with markup objects attached to ranges, like android.text.Spanned. */
public interface Spanned : CharSequence {
    /** Spans of [type] that overlap [start] to [end], in the order they were set. */
    public fun <T : Any> getSpans(start: Int, end: Int, type: kotlin.reflect.KClass<T>): List<T>
    public fun getSpanStart(span: Any): Int
    public fun getSpanEnd(span: Any): Int
    public fun getSpanFlags(span: Any): Int

    public companion object {
        public const val SPAN_INCLUSIVE_EXCLUSIVE: Int = 17
        public const val SPAN_INCLUSIVE_INCLUSIVE: Int = 18
        public const val SPAN_EXCLUSIVE_EXCLUSIVE: Int = 33
        public const val SPAN_EXCLUSIVE_INCLUSIVE: Int = 34
    }
}

/**
 * Immutable text with spans, like android.text.SpannableString: for example a pie chart's center text with
 * parts in other sizes, colors and styles.
 *
 * ```
 * val s = SpannableString("MPAndroidChart\ndeveloped by Philipp Jahoda")
 * s.setSpan(RelativeSizeSpan(1.7f), 0, 14, 0)
 * s.setSpan(ForegroundColorSpan(Color.GRAY), 14, s.length - 15, 0)
 * ```
 */
public class SpannableString(source: CharSequence) : Spanned {

    private class Entry(val span: Any, val start: Int, val end: Int, val flags: Int)

    private val string: String = source.toString()
    private val entries = mutableListOf<Entry>()

    init {
        if (source is Spanned) {
            for (span in source.getSpans(0, source.length, Any::class)) {
                entries.add(Entry(span, source.getSpanStart(span), source.getSpanEnd(span), source.getSpanFlags(span)))
            }
        }
    }

    override val length: Int get() = string.length

    override fun get(index: Int): Char = string[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex == 0 && endIndex == length) return this
        val result = SpannableString(string.substring(startIndex, endIndex))
        for (e in entries) {
            val start = maxOf(e.start, startIndex)
            val end = minOf(e.end, endIndex)
            if (start < end || (start == end && e.start == e.end && start in startIndex..endIndex)) {
                result.entries.add(Entry(e.span, start - startIndex, end - startIndex, e.flags))
            }
        }
        return result
    }

    /** Attaches [what] to the characters from [start] to [end]; setting a span again moves it. */
    public fun setSpan(what: Any, start: Int, end: Int, flags: Int) {
        require(start in 0..end && end <= length) { "setSpan ($start ... $end) ends beyond length $length" }
        val index = entries.indexOfFirst { it.span === what }
        val entry = Entry(what, start, end, flags)
        if (index >= 0) entries[index] = entry else entries.add(entry)
    }

    public fun removeSpan(what: Any) {
        entries.removeAll { it.span === what }
    }

    override fun <T : Any> getSpans(start: Int, end: Int, type: kotlin.reflect.KClass<T>): List<T> =
        entries.filter { type.isInstance(it.span) && it.start <= end && it.end >= start && (it.start < end || it.start == it.end || start == end) }
            .map {
                @Suppress("UNCHECKED_CAST")
                it.span as T
            }

    override fun getSpanStart(span: Any): Int = entries.firstOrNull { it.span === span }?.start ?: -1

    override fun getSpanEnd(span: Any): Int = entries.firstOrNull { it.span === span }?.end ?: -1

    override fun getSpanFlags(span: Any): Int = entries.firstOrNull { it.span === span }?.flags ?: 0

    override fun toString(): String = string
}

/**
 * A span that changes how characters are drawn, like android.text.style.CharacterStyle. Only the spans in this
 * package are supported, since on Android they become the matching android.text.style spans.
 */
public abstract class CharacterStyle internal constructor() {
    public abstract fun updateDrawState(paint: TextPaint)
}

/** A span that changes the size or shape of characters, like android.text.style.MetricAffectingSpan. */
public abstract class MetricAffectingSpan internal constructor() : CharacterStyle() {
    public abstract fun updateMeasureState(paint: TextPaint)
    override fun updateDrawState(paint: TextPaint): Unit = updateMeasureState(paint)
}

/** Scales the text size by [sizeChange], like android.text.style.RelativeSizeSpan. */
public class RelativeSizeSpan(public val sizeChange: Float) : MetricAffectingSpan() {
    override fun updateMeasureState(paint: TextPaint) {
        paint.textSize *= sizeChange
    }
}

/** Sets the text size to [size] pixels, or dp when [dip] is true, like android.text.style.AbsoluteSizeSpan. */
public class AbsoluteSizeSpan(public val size: Int, public val dip: Boolean = false) : MetricAffectingSpan() {
    override fun updateMeasureState(paint: TextPaint) {
        paint.textSize = if (dip) com.github.mikephil.charting.utils.Utils.convertDpToPixel(size.toFloat()) else size.toFloat()
    }
}

/** Draws the text in [foregroundColor], an ARGB int, like android.text.style.ForegroundColorSpan. */
public class ForegroundColorSpan(public val foregroundColor: Int) : CharacterStyle() {
    override fun updateDrawState(paint: TextPaint) {
        paint.color = foregroundColor
    }
}

/**
 * Adds [style] ([Typeface.BOLD], [Typeface.ITALIC], [Typeface.BOLD_ITALIC] or [Typeface.NORMAL]) to the text's
 * typeface, like android.text.style.StyleSpan.
 */
public class StyleSpan(public val style: Int) : MetricAffectingSpan() {
    override fun updateMeasureState(paint: TextPaint) {
        val old = paint.typeface
        val want = (old?.style ?: 0) or style
        paint.typeface = Typeface.create(old, want)
    }
}

/** Sets the font family, such as `serif` or `monospace`, keeping the style, like android.text.style.TypefaceSpan. */
public class TypefaceSpan(public val family: String?) : MetricAffectingSpan() {
    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = Typeface.create(family, paint.typeface?.style ?: 0)
    }
}
