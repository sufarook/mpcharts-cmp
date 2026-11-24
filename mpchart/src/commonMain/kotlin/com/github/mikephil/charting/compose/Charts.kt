package com.github.mikephil.charting.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.charts.BarChart as BarChartView
import com.github.mikephil.charting.charts.CombinedChart as CombinedChartView
import com.github.mikephil.charting.charts.HorizontalBarChart as HorizontalBarChartView
import com.github.mikephil.charting.charts.LineChart as LineChartView
import com.github.mikephil.charting.charts.PieChart as PieChartView
import com.github.mikephil.charting.charts.RadarChart as RadarChartView

/**
 * Shows a line chart backed by a [LineChartView], MPAndroidChart's own chart drawn by the same code on Android, iOS and desktop. Configure the view once in [setup] and put anything derived
 * from theme or Compose state into [update].
 *
 * @param data [LineData] to show. A new instance is applied to the chart, the same instance is left
 *   alone; call [ChartState.notifyDataChanged] after changing it in place. Null clears the chart.
 * @param modifier modifier applied to the chart, usually giving it a size.
 * @param animateChanges whether a new [data] instance animates in from the values shown before, over 300 ms, instead
 *   of replacing them at once; see [ChartState.animateDataChange]. Charts without data and data of another shape
 *   are set without animation.
 * @param state reports selection and viewport back to Compose and drives the chart. Use one state per chart; passing it to another chart moves it there.
 * @param contentDescription text read by screen readers, null for none.
 * @param marker composable drawn at the highlighted entry, null for no marker.
 * @param setup runs once after the view is created and before any data is set, so values it reads are frozen.
 * @param update runs when [data], [state], [contentDescription], [marker] or the lambda itself change, and when Compose state it reads changes, right before the chart redraws.
 */
@Composable
public fun LineChart(
    data: LineData?,
    modifier: Modifier = Modifier,
    state: ChartState = rememberChartState(),
    contentDescription: String? = null,
    marker: (@Composable (Entry<*>, Highlight) -> Unit)? = null,
    animateChanges: Boolean = false,
    setup: LineChartView.() -> Unit = {},
    update: LineChartView.() -> Unit = {},
): Unit = ChartHost(data, modifier, animateChanges, state, contentDescription, marker, ::LineChartView, setup, update)

/**
 * Shows a bar chart with vertical bars backed by a [BarChartView]. Configure the view once in [setup]
 * and put anything derived from theme or Compose state into [update].
 *
 * @param data [BarData] to show. A new instance is applied to the chart, the same instance is left
 *   alone; call [ChartState.notifyDataChanged] after changing it in place. Null clears the chart.
 * @param modifier modifier applied to the chart, usually giving it a size.
 * @param animateChanges whether a new [data] instance animates in from the values shown before, over 300 ms, instead
 *   of replacing them at once; see [ChartState.animateDataChange]. Charts without data and data of another shape
 *   are set without animation.
 * @param state reports selection and viewport back to Compose and drives the chart. Use one state per chart; passing it to another chart moves it there.
 * @param contentDescription text read by screen readers, null for none.
 * @param marker composable drawn at the highlighted entry, null for no marker.
 * @param setup runs once after the view is created and before any data is set, so values it reads are frozen.
 * @param update runs when [data], [state], [contentDescription], [marker] or the lambda itself change, and when Compose state it reads changes, right before the chart redraws.
 */
@Composable
public fun BarChart(
    data: BarData?,
    modifier: Modifier = Modifier,
    state: ChartState = rememberChartState(),
    contentDescription: String? = null,
    marker: (@Composable (Entry<*>, Highlight) -> Unit)? = null,
    animateChanges: Boolean = false,
    setup: BarChartView.() -> Unit = {},
    update: BarChartView.() -> Unit = {},
): Unit = ChartHost(data, modifier, animateChanges, state, contentDescription, marker, ::BarChartView, setup, update)

/**
 * Shows a bar chart with horizontal bars backed by a [HorizontalBarChartView]. Configure the view once
 * in [setup] and put anything derived from theme or Compose state into [update].
 *
 * @param data [BarData] to show. A new instance is applied to the chart, the same instance is left
 *   alone; call [ChartState.notifyDataChanged] after changing it in place. Null clears the chart.
 * @param modifier modifier applied to the chart, usually giving it a size.
 * @param animateChanges whether a new [data] instance animates in from the values shown before, over 300 ms, instead
 *   of replacing them at once; see [ChartState.animateDataChange]. Charts without data and data of another shape
 *   are set without animation.
 * @param state reports selection and viewport back to Compose and drives the chart. Use one state per chart; passing it to another chart moves it there.
 * @param contentDescription text read by screen readers, null for none.
 * @param marker composable drawn at the highlighted entry, null for no marker.
 * @param setup runs once after the view is created and before any data is set, so values it reads are frozen.
 * @param update runs when [data], [state], [contentDescription], [marker] or the lambda itself change, and when Compose state it reads changes, right before the chart redraws.
 */
@Composable
public fun HorizontalBarChart(
    data: BarData?,
    modifier: Modifier = Modifier,
    state: ChartState = rememberChartState(),
    contentDescription: String? = null,
    marker: (@Composable (Entry<*>, Highlight) -> Unit)? = null,
    animateChanges: Boolean = false,
    setup: HorizontalBarChartView.() -> Unit = {},
    update: HorizontalBarChartView.() -> Unit = {},
): Unit = ChartHost(data, modifier, animateChanges, state, contentDescription, marker, ::HorizontalBarChartView, setup, update)

/**
 * Shows a pie chart backed by a [PieChartView]. Configure the view once in [setup] and put anything derived
 * from theme or Compose state into [update].
 *
 * @param data [PieData] to show. A new instance is applied to the chart, the same instance is left
 *   alone; call [ChartState.notifyDataChanged] after changing it in place. Null clears the chart.
 * @param modifier modifier applied to the chart, usually giving it a size.
 * @param animateChanges whether a new [data] instance animates in from the values shown before, over 300 ms, instead
 *   of replacing them at once; see [ChartState.animateDataChange]. Charts without data and data of another shape
 *   are set without animation.
 * @param state reports selection and viewport back to Compose and drives the chart. Use one state per chart; passing it to another chart moves it there.
 * @param contentDescription text read by screen readers, null for none.
 * @param marker composable drawn at the highlighted entry, null for no marker.
 * @param setup runs once after the view is created and before any data is set, so values it reads are frozen.
 * @param update runs when [data], [state], [contentDescription], [marker] or the lambda itself change, and when Compose state it reads changes, right before the chart redraws.
 */
@Composable
public fun PieChart(
    data: PieData?,
    modifier: Modifier = Modifier,
    state: ChartState = rememberChartState(),
    contentDescription: String? = null,
    marker: (@Composable (Entry<*>, Highlight) -> Unit)? = null,
    animateChanges: Boolean = false,
    setup: PieChartView.() -> Unit = {},
    update: PieChartView.() -> Unit = {},
): Unit = ChartHost(data, modifier, animateChanges, state, contentDescription, marker, ::PieChartView, setup, update)

/**
 * Shows a radar chart backed by a [RadarChartView]. Configure the view once in [setup] and put anything derived
 * from theme or Compose state into [update].
 *
 * @param data [RadarData] to show. A new instance is applied to the chart, the same instance is left
 *   alone; call [ChartState.notifyDataChanged] after changing it in place. Null clears the chart.
 * @param modifier modifier applied to the chart, usually giving it a size.
 * @param animateChanges whether a new [data] instance animates in from the values shown before, over 300 ms, instead
 *   of replacing them at once; see [ChartState.animateDataChange]. Charts without data and data of another shape
 *   are set without animation.
 * @param state reports selection and viewport back to Compose and drives the chart. Use one state per chart; passing it to another chart moves it there.
 * @param contentDescription text read by screen readers, null for none.
 * @param marker composable drawn at the highlighted entry, null for no marker.
 * @param setup runs once after the view is created and before any data is set, so values it reads are frozen.
 * @param update runs when [data], [state], [contentDescription], [marker] or the lambda itself change, and when Compose state it reads changes, right before the chart redraws.
 */
@Composable
public fun RadarChart(
    data: RadarData?,
    modifier: Modifier = Modifier,
    state: ChartState = rememberChartState(),
    contentDescription: String? = null,
    marker: (@Composable (Entry<*>, Highlight) -> Unit)? = null,
    animateChanges: Boolean = false,
    setup: RadarChartView.() -> Unit = {},
    update: RadarChartView.() -> Unit = {},
): Unit = ChartHost(data, modifier, animateChanges, state, contentDescription, marker, ::RadarChartView, setup, update)

/**
 * Shows a chart that layers several chart types backed by a [CombinedChartView]. Configure the view
 * once in [setup] and put anything derived from theme or Compose state into [update].
 *
 * @param data [CombinedData] to show. A new instance is applied to the chart, the same instance is left
 *   alone; call [ChartState.notifyDataChanged] after changing it in place. Null clears the chart.
 * @param modifier modifier applied to the chart, usually giving it a size.
 * @param animateChanges whether a new [data] instance animates in from the values shown before, over 300 ms, instead
 *   of replacing them at once; see [ChartState.animateDataChange]. Charts without data and data of another shape
 *   are set without animation.
 * @param state reports selection and viewport back to Compose and drives the chart. Use one state per chart; passing it to another chart moves it there.
 * @param contentDescription text read by screen readers, null for none.
 * @param marker composable drawn at the highlighted entry, null for no marker.
 * @param setup runs once after the view is created and before any data is set, so values it reads are frozen.
 * @param update runs when [data], [state], [contentDescription], [marker] or the lambda itself change, and when Compose state it reads changes, right before the chart redraws.
 */
@Composable
public fun CombinedChart(
    data: CombinedData?,
    modifier: Modifier = Modifier,
    state: ChartState = rememberChartState(),
    contentDescription: String? = null,
    marker: (@Composable (Entry<*>, Highlight) -> Unit)? = null,
    animateChanges: Boolean = false,
    setup: CombinedChartView.() -> Unit = {},
    update: CombinedChartView.() -> Unit = {},
): Unit = ChartHost(data, modifier, animateChanges, state, contentDescription, marker, ::CombinedChartView, setup, update)
