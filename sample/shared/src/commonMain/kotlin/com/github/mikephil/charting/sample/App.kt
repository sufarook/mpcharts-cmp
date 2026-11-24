package com.github.mikephil.charting.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.github.mikephil.charting.compose.ComposeMarker
import com.github.mikephil.charting.platform.DecimalFormat
import com.github.mikephil.charting.platform.ForegroundColorSpan
import com.github.mikephil.charting.platform.RelativeSizeSpan
import com.github.mikephil.charting.platform.SpannableString
import com.github.mikephil.charting.platform.StyleSpan
import com.github.mikephil.charting.platform.Typeface
import com.github.mikephil.charting.sample.resources.Res
import com.github.mikephil.charting.sample.resources.marker2
import com.github.mikephil.charting.sample.resources.radar_marker
import com.github.mikephil.charting.sample.resources.roboto_regular
import com.github.mikephil.charting.utils.Utils
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.imageResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.compose.BarChart
import com.github.mikephil.charting.compose.CombinedChart
import com.github.mikephil.charting.compose.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.charts.CombinedChart as CombinedChartView
import com.github.mikephil.charting.data.BubbleData
import com.github.mikephil.charting.data.BubbleDataSet
import com.github.mikephil.charting.data.BubbleEntry
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.compose.HorizontalBarChart
import com.github.mikephil.charting.compose.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.platform.DashPathEffect
import com.github.mikephil.charting.platform.GradientDrawable
import com.github.mikephil.charting.compose.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.platform.Color as ChartColor
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.Fill
import com.github.mikephil.charting.utils.MPPointF
import kotlin.random.Random

private enum class Screen(val title: String) { BAR("Bar"), HBAR("H-Bar"), PIE("Pie"), LINE("Line"), COMBINED("Mix"), RADAR("Radar") }

/** The sample app: MPAndroidChart's bar and pie example screens, the same on Android, iOS and desktop. */
@Composable
public fun App(startScreen: String = "bar") {
    var screen by remember { mutableStateOf(Screen.entries.firstOrNull { it.name.equals(startScreen, ignoreCase = true) } ?: Screen.BAR) }
    // a fixed seed makes every platform show the same data, for side by side comparison
    var seed by remember { mutableIntStateOf(42) }
    var animateRequest by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF33B5E5)).horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in Screen.entries) {
                Chip(s.title, selected = s == screen) { screen = s }
            }
            Box(Modifier.padding(horizontal = 8.dp))
            Chip("Animate", selected = false) { animateRequest++ }
            Chip("Data", selected = false) { seed++ }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                Screen.BAR -> BarScreen(seed, animateRequest)
                Screen.HBAR -> HorizontalBarScreen(seed, animateRequest)
                Screen.PIE -> PieScreen(seed, animateRequest)
                Screen.LINE -> LineScreen(seed, animateRequest)
                Screen.COMBINED -> CombinedScreen(seed, animateRequest)
                Screen.RADAR -> RadarScreen(seed, animateRequest)
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    BasicText(
        text,
        style = TextStyle(color = if (selected) Color(0xFF33B5E5) else Color.White, fontSize = 14.sp),
        modifier = Modifier
            .background(if (selected) Color.White else Color.Transparent)
            .border(1.dp, Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// android.R.color holo colors used by the example's gradient fills
private val holoOrangeLight = 0xFFFFBB33.toInt()
private val holoBlueLight = 0xFF33B5E5.toInt()
private val holoGreenLight = 0xFF99CC00.toInt()
private val holoRedLight = 0xFFFF4444.toInt()
private val holoBlueDark = 0xFF0099CC.toInt()
private val holoPurple = 0xFFAA66CC.toInt()
private val holoGreenDark = 0xFF669900.toInt()
private val holoRedDark = 0xFFCC0000.toInt()
private val holoOrangeDark = 0xFFFF8800.toInt()

private fun barData(seed: Int, count: Int = 12, range: Float = 50f): BarData {
    val random = Random(seed)
    val values = (1..count).map { i -> BarEntry(i.toFloat(), random.nextFloat() * (range + 1)) }
    val set = BarDataSet(values, "The year 2017")
    set.isDrawIconsEnabled = false
    set.fills = listOf(
        Fill(holoOrangeLight, holoBlueDark),
        Fill(holoBlueLight, holoPurple),
        Fill(holoOrangeLight, holoGreenDark),
        Fill(holoGreenLight, holoRedDark),
        Fill(holoRedLight, holoOrangeDark),
    )
    val data = BarData(listOf(set))
    data.setValueTextSize(10f)
    data.barWidth = 0.9f
    return data
}

@Composable
private fun BarScreen(seed: Int, animateRequest: Int) {
    val data = remember(seed) { barData(seed) }
    BarChart(
        data = data,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Bar chart",
        marker = { entry, _ ->
            // the example's XYMarkerView
            val x = chartXAxis?.let { it.valueFormatter.getFormattedValue(entry.x, it) } ?: entry.x.toString()
            ExampleMarker("x: $x, y: ${xyFormat.format(entry.y.toDouble())}", Res.drawable.marker2, textSize = 12.sp, marginTop = 7.dp)
        },
        setup = {
            isDrawBarShadowEnabled = false
            isDrawValueAboveBarEnabled = true
            description.isEnabled = false
            maxVisibleCount = 60
            isPinchZoomEnabled = false
            isDrawGridBackgroundEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                isDrawGridLinesEnabled = false
                granularity = 1f
                labelCount = 7
            }
            axisLeft.apply {
                labelCount = 8
                labelPosition = YAxis.YAxisLabelPosition.OUTSIDE_CHART
                spaceTop = 15f
                axisMinimum = 0f
            }
            axisRight.apply {
                isDrawGridLinesEnabled = false
                labelCount = 8
                spaceTop = 15f
                axisMinimum = 0f
            }
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                isDrawInsideEnabled = false
                form = Legend.LegendForm.SQUARE
                formSize = 9f
                textSize = 11f
                xEntrySpace = 4f
            }
        },
        update = {
            chartXAxis = xAxis
            // the example marker sits right above the entry
            (marker as? ComposeMarker)?.setOffset(0f, 12f)
            if (animateRequest > 0 && lastAnimateRequest[this] != animateRequest) {
                lastAnimateRequest[this] = animateRequest
                animateY(1400, Easing.EaseInOutQuad)
            }
        },
    )
}

private val xyFormat = DecimalFormat("###.0")
private var chartXAxis: XAxis? = null

/**
 * The example apps' marker views: a 40 dp high RelativeLayout with a nodpi image as background, whose pixel size is
 * its minimum size, and a single line white TextView centered at the top.
 */
@Composable
private fun ExampleMarker(text: String, background: DrawableResource, textSize: TextUnit, marginTop: Dp, width: Dp? = null) {
    val image = imageResource(background)
    val density = LocalDensity.current
    val minWidth = with(density) { image.width.toDp() }
    val size = if (width != null) Modifier.width(width) else Modifier.widthIn(min = minWidth)
    Box(
        size.height(40.dp).drawBehind {
            drawImage(image, dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()), dstOffset = IntOffset.Zero)
        },
    ) {
        BasicText(
            text,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = marginTop, start = 5.dp, end = 5.dp),
            style = TextStyle(color = Color.White, fontSize = textSize, fontFamily = FontFamily(Font(Res.font.roboto_regular))),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun horizontalBarData(seed: Int, count: Int = 12, range: Float = 50f): BarData {
    val random = Random(seed)
    val spaceForBar = 10f
    val values = (0 until count).map { i -> BarEntry(i * spaceForBar, random.nextFloat() * range) }
    val set = BarDataSet(values, "DataSet 1")
    set.isDrawIconsEnabled = false
    val data = BarData(listOf(set))
    data.setValueTextSize(10f)
    data.barWidth = 9f
    return data
}

@Composable
private fun HorizontalBarScreen(seed: Int, animateRequest: Int) {
    val data = remember(seed) { horizontalBarData(seed) }
    HorizontalBarChart(
        data = data,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Horizontal bar chart",
        setup = {
            isDrawBarShadowEnabled = false
            isDrawValueAboveBarEnabled = true
            description.isEnabled = false
            maxVisibleCount = 60
            isPinchZoomEnabled = false
            isDrawGridBackgroundEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                isDrawAxisLineEnabled = true
                isDrawGridLinesEnabled = false
                granularity = 10f
            }
            axisLeft.apply {
                isDrawAxisLineEnabled = true
                isDrawGridLinesEnabled = true
                axisMinimum = 0f
            }
            axisRight.apply {
                isDrawAxisLineEnabled = true
                isDrawGridLinesEnabled = false
                axisMinimum = 0f
            }
            isFitBarsEnabled = true
            animateY(2500)
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                isDrawInsideEnabled = false
                formSize = 8f
                xEntrySpace = 4f
            }
        },
        update = {
            if (animateRequest > 0 && lastAnimateRequest[this] != animateRequest) {
                lastAnimateRequest[this] = animateRequest
                animateY(2500)
            }
        },
    )
}

private val parties = listOf(
    "Party A", "Party B", "Party C", "Party D", "Party E", "Party F", "Party G", "Party H", "Party I", "Party J",
)

private fun pieData(seed: Int, count: Int = 5, range: Float = 10f): PieData {
    val random = Random(seed)
    val entries = (0 until count).map { i -> PieEntry(random.nextFloat() * range + range / 5, parties[i % parties.size]) }
    val set = PieDataSet(entries, "Election Results")
    set.isDrawIconsEnabled = false
    set.sliceSpace = 3f
    set.iconsOffset = MPPointF(0f, 40f)
    set.selectionShift = 5f
    set.colors = ColorTemplate.VORDIPLOM_COLORS + ColorTemplate.JOYFUL_COLORS + ColorTemplate.COLORFUL_COLORS +
        ColorTemplate.LIBERTY_COLORS + ColorTemplate.PASTEL_COLORS + ColorTemplate.holoBlue
    val data = PieData(set)
    data.setValueFormatter(PercentFormatter())
    data.setValueTextSize(11f)
    data.setValueTextColor(ChartColor.WHITE)
    return data
}

/** The example's center text: a large title, a smaller gray line and an italic blue name. */
private fun generateCenterSpannableText(): SpannableString {
    val s = SpannableString("MPAndroidChart\ndeveloped by Philipp Jahoda")
    s.setSpan(RelativeSizeSpan(1.7f), 0, 14, 0)
    s.setSpan(StyleSpan(Typeface.NORMAL), 14, s.length - 15, 0)
    s.setSpan(ForegroundColorSpan(ChartColor.GRAY), 14, s.length - 15, 0)
    s.setSpan(RelativeSizeSpan(.8f), 14, s.length - 15, 0)
    s.setSpan(StyleSpan(Typeface.ITALIC), s.length - 14, s.length, 0)
    s.setSpan(ForegroundColorSpan(ColorTemplate.holoBlue), s.length - 14, s.length, 0)
    return s
}

@Composable
private fun PieScreen(seed: Int, animateRequest: Int) {
    val data = remember(seed) { pieData(seed) }
    PieChart(
        data = data,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Pie chart",
        setup = {
            isUsePercentValuesEnabled = true
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            centerText = generateCenterSpannableText()
            isDrawHoleEnabled = true
            holeColor = ChartColor.WHITE
            transparentCircleColor = ChartColor.WHITE
            transparentCircleAlpha = 110
            holeRadius = 58f
            transparentCircleRadius = 61f
            isDrawCenterTextEnabled = true
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                isDrawInsideEnabled = false
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
            }
            entryLabelColor = ChartColor.WHITE
            entryLabelTextSize = 12f
        },
        update = {
            if (animateRequest > 0 && lastAnimateRequest[this] != animateRequest) {
                lastAnimateRequest[this] = animateRequest
                animateY(1400, Easing.EaseInOutQuad)
            }
        },
    )
}

private fun lineData(seed: Int, count: Int = 45, range: Float = 180f): LineData {
    val random = Random(seed)
    val values = (0 until count).map { i -> Entry(i.toFloat(), random.nextFloat() * range - 30) }
    val set = LineDataSet(values, "DataSet 1").apply {
        isDrawIconsEnabled = false
        enableDashedLine(10f, 5f, 0f)
        color = ChartColor.BLACK
        circleColor = ChartColor.BLACK
        lineWidth = 1f
        circleRadius = 3f
        isDrawCircleHoleEnabled = false
        formLineWidth = 1f
        formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        formSize = 15f
        valueTextSize = 9f
        enableDashedHighlightLine(10f, 5f, 0f)
        isDrawFilledEnabled = true
        fillFormatter = IFillFormatter { _, _ -> -50f }
        // the example's fade_red drawable: <gradient android:angle="90" startColor="#00ff0000" endColor="#ffff0000"/>
        fillDrawable = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0x00FF0000, 0xFFFF0000.toInt()))
    }
    return LineData(listOf(set))
}

@Composable
private fun LineScreen(seed: Int, animateRequest: Int) {
    val data = remember(seed) { lineData(seed) }
    LineChart(
        data = data,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Line chart",
        marker = { entry, _ ->
            // the example's MyMarkerView
            val value = if (entry is CandleEntry<*>) entry.high else entry.y
            ExampleMarker(Utils.formatNumber(value, 0, true), Res.drawable.marker2, textSize = 12.sp, marginTop = 7.dp)
        },
        setup = {
            description.isEnabled = false
            isTouchEnabled = true
            isDrawGridBackgroundEnabled = false
            isDragEnabled = true
            isScaleEnabled = true
            isPinchZoomEnabled = true

            xAxis.enableGridDashedLine(10f, 10f, 0f)
            axisRight.isEnabled = false
            axisLeft.apply {
                enableGridDashedLine(10f, 10f, 0f)
                axisMaximum = 200f
                axisMinimum = -50f
                addLimitLine(LimitLine(150f, "Upper Limit").apply {
                    lineWidth = 4f
                    enableDashedLine(10f, 10f, 0f)
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                    textSize = 10f
                })
                addLimitLine(LimitLine(-30f, "Lower Limit").apply {
                    lineWidth = 4f
                    enableDashedLine(10f, 10f, 0f)
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                    textSize = 10f
                })
                isDrawLimitLinesBehindDataEnabled = true
            }
            xAxis.isDrawLimitLinesBehindDataEnabled = true
            legend.form = Legend.LegendForm.LINE
            animateX(1500)
        },
        update = {
            // the example marker sits right above the entry
            (marker as? ComposeMarker)?.setOffset(0f, 12f)
            if (animateRequest > 0 && lastAnimateRequest[this] != animateRequest) {
                lastAnimateRequest[this] = animateRequest
                animateX(1500)
            }
        },
    )
}

private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** MPAndroidChart's CombinedChartActivity data: line, grouped and stacked bars, scatter, candles and bubbles. */
private fun combinedData(seed: Int, count: Int = 12): CombinedData {
    val random = Random(seed)
    fun getRandom(range: Float, start: Float) = random.nextFloat() * range + start

    val line = LineDataSet((0 until count).map { Entry(it + 0.5f, getRandom(15f, 5f)) }, "Line DataSet").apply {
        color = ChartColor.rgb(240, 238, 70)
        lineWidth = 2.5f
        circleColor = ChartColor.rgb(240, 238, 70)
        circleRadius = 5f
        fillColor = ChartColor.rgb(240, 238, 70)
        mode = LineDataSet.Mode.CUBIC_BEZIER
        isDrawValuesEnabled = true
        valueTextSize = 10f
        valueTextColor = ChartColor.rgb(240, 238, 70)
        axisDependency = YAxis.AxisDependency.LEFT
    }

    val entries1 = mutableListOf<BarEntry<Nothing>>()
    val entries2 = mutableListOf<BarEntry<Nothing>>()
    repeat(count) {
        entries1.add(BarEntry(0f, getRandom(25f, 25f)))
        entries2.add(BarEntry(0f, listOf(getRandom(13f, 12f), getRandom(13f, 12f))))
    }
    val bar1 = BarDataSet(entries1, "Bar 1").apply {
        color = ChartColor.rgb(60, 220, 78)
        valueTextColor = ChartColor.rgb(60, 220, 78)
        valueTextSize = 10f
        axisDependency = YAxis.AxisDependency.LEFT
    }
    val bar2 = BarDataSet(entries2, "").apply {
        stackLabels = listOf("Stack 1", "Stack 2")
        setColors(ChartColor.rgb(61, 165, 255), ChartColor.rgb(23, 197, 255))
        valueTextColor = ChartColor.rgb(61, 165, 255)
        valueTextSize = 10f
        axisDependency = YAxis.AxisDependency.LEFT
    }
    val barData = BarData(bar1, bar2).apply {
        barWidth = 0.45f
        groupBars(0f, 0.06f, 0.02f)
    }

    val scatterEntries = mutableListOf<Entry<Nothing>>()
    var index = 0f
    while (index < count) {
        scatterEntries.add(Entry(index + 0.25f, getRandom(10f, 55f)))
        index += 0.5f
    }
    val scatter = ScatterDataSet(scatterEntries, "Scatter DataSet").apply {
        colors = ColorTemplate.MATERIAL_COLORS
        scatterShapeSize = 3.75f
        isDrawValuesEnabled = false
        valueTextSize = 10f
    }

    val candle = CandleDataSet((0 until count step 2).map { CandleEntry(it + 1f, 90f, 70f, 85f, 75f) }, "Candle DataSet").apply {
        decreasingColor = ChartColor.rgb(142, 150, 175)
        shadowColor = ChartColor.DKGRAY
        barSpace = 0.3f
        valueTextSize = 10f
        isDrawValuesEnabled = false
    }

    val bubble = BubbleDataSet((0 until count).map { BubbleEntry(it + 0.5f, getRandom(10f, 105f), getRandom(100f, 105f)) }, "Bubble DataSet").apply {
        colors = ColorTemplate.VORDIPLOM_COLORS
        valueTextSize = 10f
        valueTextColor = ChartColor.WHITE
        highlightCircleWidth = 1.5f
        isDrawValuesEnabled = true
    }

    return CombinedData().apply {
        lineData = LineData(listOf(line))
        this.barData = barData
        bubbleData = BubbleData(listOf(bubble))
        scatterData = ScatterData(listOf(scatter))
        candleData = CandleData(listOf(candle))
    }
}

@Composable
private fun CombinedScreen(seed: Int, animateRequest: Int) {
    val data = remember(seed) { combinedData(seed) }
    CombinedChart(
        data = data,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Combined chart",
        setup = {
            description.isEnabled = false
            isDrawGridBackgroundEnabled = false
            isDrawBarShadowEnabled = false
            isHighlightFullBarEnabled = false
            drawOrder = listOf(
                CombinedChartView.DrawOrder.BAR,
                CombinedChartView.DrawOrder.BUBBLE,
                CombinedChartView.DrawOrder.CANDLE,
                CombinedChartView.DrawOrder.LINE,
                CombinedChartView.DrawOrder.SCATTER,
            )
            legend.apply {
                isWordWrapEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                isDrawInsideEnabled = false
            }
            axisRight.apply {
                isDrawGridLinesEnabled = false
                axisMinimum = 0f
            }
            axisLeft.apply {
                isDrawGridLinesEnabled = false
                axisMinimum = 0f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTH_SIDED
                axisMinimum = 0f
                granularity = 1f
                valueFormatter = IAxisValueFormatter { value, _ -> months[value.toInt() % months.size] }
            }
        },
        update = {
            // the example sets this from the data before showing it
            xAxis.axisMaximum = data.xMax + 0.25f
            if (animateRequest > 0 && lastAnimateRequest[this] != animateRequest) {
                lastAnimateRequest[this] = animateRequest
                animateY(1400, Easing.EaseInOutQuad)
            }
        },
    )
}

private val activities = listOf("Burger", "Steak", "Salad", "Pasta", "Pizza")

/** MPAndroidChart's RadarChartActivity data: two weeks of five ratings between 20 and 100. */
private fun radarData(seed: Int, count: Int = 5): RadarData {
    val random = Random(seed)
    val entries1 = mutableListOf<RadarEntry<Nothing>>()
    val entries2 = mutableListOf<RadarEntry<Nothing>>()
    repeat(count) {
        entries1.add(RadarEntry(random.nextFloat() * 80f + 20f))
        entries2.add(RadarEntry(random.nextFloat() * 80f + 20f))
    }
    fun RadarDataSet<Nothing>.style(rgb: Int) = apply {
        color = rgb
        fillColor = rgb
        isDrawFilledEnabled = true
        fillAlpha = 180
        lineWidth = 2f
        isDrawHighlightCircleEnabled = true
        setDrawHighlightIndicators(false)
    }
    val set1 = RadarDataSet(entries1, "Last Week").style(ChartColor.rgb(103, 110, 129))
    val set2 = RadarDataSet(entries2, "This Week").style(ChartColor.rgb(121, 162, 175))
    return RadarData(listOf(set1, set2)).apply {
        setValueTextSize(8f)
        setDrawValues(false)
        setValueTextColor(ChartColor.WHITE)
    }
}

private val radarFormat = DecimalFormat("##0")

@Composable
private fun RadarScreen(seed: Int, animateRequest: Int) {
    val data = remember(seed) { radarData(seed) }
    RadarChart(
        data = data,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Radar chart",
        marker = { entry, _ ->
            // the example's RadarMarkerView
            ExampleMarker("${radarFormat.format(entry.y.toDouble())} %", Res.drawable.radar_marker, textSize = 10.sp, marginTop = 3.dp, width = 55.dp)
        },
        setup = {
            setBackgroundColor(ChartColor.rgb(60, 65, 82))
            description.isEnabled = false
            webLineWidth = 1f
            webColor = ChartColor.LTGRAY
            webLineWidthInner = 1f
            webColorInner = ChartColor.LTGRAY
            webAlpha = 100
            animateXY(1400, 1400, Easing.EaseInOutQuad)
            xAxis.apply {
                textSize = 9f
                yOffset = 0f
                xOffset = 0f
                valueFormatter = IAxisValueFormatter { value, _ -> activities[value.toInt() % activities.size] }
                textColor = ChartColor.WHITE
            }
            yAxis.apply {
                labelCount = 5
                textSize = 9f
                axisMinimum = 0f
                axisMaximum = 80f
                isDrawLabelsEnabled = false
            }
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                isDrawInsideEnabled = false
                xEntrySpace = 7f
                yEntrySpace = 5f
                textColor = ChartColor.WHITE
            }
        },
        update = {
            // the example marker sits 10 px above the entry
            (marker as? ComposeMarker)?.setOffset(0f, 12f - Utils.convertPixelsToDp(10f))
            if (animateRequest > 0 && lastAnimateRequest[this] != animateRequest) {
                lastAnimateRequest[this] = animateRequest
                animateXY(1400, 1400, Easing.EaseInOutQuad)
            }
        },
    )
}

/** Which animate request each chart has run, so a recomposition does not restart the animation. */
private val lastAnimateRequest = HashMap<Any, Int>()

