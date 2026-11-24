package com.github.mikephil.charting.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.platform.AnimationClock
import com.github.mikephil.charting.platform.MotionEvent
import com.github.mikephil.charting.platform.ViewHost
import com.github.mikephil.charting.utils.Utils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive

/** How long a chart composable with `animateChanges` animates a data change, in milliseconds. */
internal const val CHANGE_ANIMATION_MILLIS = 300

private class HostState<D> {
    var appliedData: D? = null
    var hasAppliedData = false
    var disallowIntercept = false
    var state: ChartState? = null
}

/**
 * Hosts a chart inside Compose; the public chart composables in Charts.kt delegate here.
 * [setup] runs once when the chart is created and before any data is set, so values it reads are frozen.
 * [update] runs when [data], [state], [contentDescription], [marker] or the lambda itself change, and when
 * Compose state it reads changes. A new [data] instance is applied to the chart; the same instance is
 * left alone, call [ChartState.notifyDataChanged] after changing it in place. With [animateChanges] a new
 * [data] instance animates in through [Chart.animateDataChange] over [CHANGE_ANIMATION_MILLIS].
 * [marker] is wrapped in a [ComposeMarker] that is reused while the chart lives.
 */
@Composable
internal fun <C : Chart<D>, D : ChartData<out IDataSet<out Entry<*>>>> ChartHost(
    data: D?,
    modifier: Modifier,
    animateChanges: Boolean,
    state: ChartState,
    contentDescription: String?,
    marker: (@Composable (Entry<*>, Highlight) -> Unit)?,
    create: () -> C,
    setup: C.() -> Unit,
    update: C.() -> Unit,
) {
    val semanticsModifier = if (contentDescription == null) modifier else modifier.semantics { this.contentDescription = contentDescription }
    BindPlatformContext()
    if (!rememberChartFontsReady()) {
        Box(semanticsModifier)
        return
    }

    val density = LocalDensity.current.density
    val chart = remember {
        Utils.init(density)
        create().apply(setup)
    }
    val host = remember { HostState<D>() }
    var drawRequests by remember { mutableIntStateOf(0) }
    val frames = remember { Channel<Unit>(Channel.CONFLATED) }
    val composeMarker = remember(chart) { ComposeMarker(chart) { _, _ -> } }

    DisposableEffect(chart) {
        val viewHost = object : ViewHost {
            override fun invalidate() {
                drawRequests++
            }

            override fun requestFrame() {
                frames.trySend(Unit)
            }

            override fun requestDisallowInterceptTouchEvent(disallow: Boolean) {
                host.disallowIntercept = disallow
            }
        }
        val waker = { frames.trySend(Unit); Unit }
        AnimationClock.addWaker(waker)
        chart.attach(viewHost)
        onDispose {
            AnimationClock.removeWaker(waker)
            host.state?.detach(chart)
            host.state = null
            composeMarker.detach()
            chart.detach()
        }
    }

    LaunchedEffect(chart) {
        while (isActive) {
            if (chart.needsFrames) withFrameMillis { chart.onFrame() } else frames.receive()
        }
    }

    SideEffect {
        Utils.init(density)
        if (host.state !== state) {
            host.state?.detach(chart)
            host.state = state
            state.attach(chart)
        }
        if (!host.hasAppliedData || host.appliedData !== data) {
            if (animateChanges && data != null && host.hasAppliedData) {
                chart.animateDataChange(data, CHANGE_ANIMATION_MILLIS) { state.syncSelection() }
            } else {
                chart.data = data
            }
            host.appliedData = data
            host.hasAppliedData = true
            state.syncSelection()
        }
        if (marker == null) {
            composeMarker.detach()
        } else {
            composeMarker.content = marker
            chart.marker = composeMarker
        }
        chart.update()
        chart.invalidate()
        state.refresh()
    }

    Box(semanticsModifier) {
        Spacer(
            Modifier
                .matchParentSize()
                .onSizeChanged { chart.setSize(it.width, it.height) }
                .pointerInput(chart) { forwardTouches(chart, host) }
                .drawBehind {
                    drawRequests // read so that an invalidate redraws
                    chart.setSize(size.width.toInt(), size.height.toInt())
                    composeMarker.positions.clear()
                    drawIntoCanvas { canvas -> chart.draw(canvas.toChartCanvas(size.width.toInt(), size.height.toInt())) }
                    host.state?.refresh()
                },
        )
        if (marker != null) MarkerLayer(chart, composeMarker) { drawRequests }
    }
}

/**
 * Composes the marker content unmeasured by the chart's size and draws it at every position the chart drew its
 * marker at in this frame; the chart draws first, so the positions are current.
 */
@Composable
private fun MarkerLayer(chart: Chart<*>, marker: ComposeMarker, drawRequests: () -> Int) {
    Box(
        Modifier
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .onSizeChanged {
                if (marker.width != it.width || marker.height != it.height) {
                    marker.width = it.width
                    marker.height = it.height
                    chart.invalidate()
                }
            }
            .drawWithContent {
                drawRequests()
                val positions = marker.positions
                var i = 0
                while (i + 1 < positions.size) {
                    translate(positions[i], positions[i + 1]) { this@drawWithContent.drawContent() }
                    i += 2
                }
            },
    ) {
        val current = marker.shown ?: return@Box
        marker.content(current.first, current.second)
        SideEffect {
            if (marker.composedFor !== current) {
                marker.composedFor = current
                chart.invalidate()
            }
        }
    }
}

/** Turns Compose pointer events into Android style [MotionEvent]s for the chart's touch listener. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.forwardTouches(chart: Chart<*>, state: HostState<*>) {
    awaitPointerEventScope {
        // pointers in index order, each with the smallest free id, as on Android
        val pointers = mutableListOf<Pair<PointerId, Int>>()
        val positions = HashMap<PointerId, androidx.compose.ui.geometry.Offset>()
        var downTime = 0L

        fun dispatch(action: Int, time: Long) {
            val ids = IntArray(pointers.size) { pointers[it].second }
            val xs = FloatArray(pointers.size) { positions[pointers[it].first]!!.x }
            val ys = FloatArray(pointers.size) { positions[pointers[it].first]!!.y }
            chart.dispatchTouchEvent(MotionEvent.obtain(downTime, time, action, ids, xs, ys))
        }

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            var moved = false
            var time = 0L
            for (change in event.changes) {
                time = change.uptimeMillis
                if (change.pressed && !change.previousPressed) {
                    val free = generateSequence(0) { it + 1 }.first { id -> pointers.none { it.second == id } }
                    positions[change.id] = change.position
                    pointers.add(change.id to free)
                    if (pointers.size == 1) {
                        downTime = time
                        state.disallowIntercept = false
                        dispatch(MotionEvent.ACTION_DOWN, time)
                    } else {
                        val index = pointers.size - 1
                        dispatch(MotionEvent.ACTION_POINTER_DOWN or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), time)
                    }
                } else if (change.pressed && change.previousPressed) {
                    if (positions[change.id] != change.position) {
                        positions[change.id] = change.position
                        moved = true
                    }
                }
            }
            if (moved && pointers.isNotEmpty()) dispatch(MotionEvent.ACTION_MOVE, time)
            for (change in event.changes) {
                if (!change.pressed && change.previousPressed) {
                    val index = pointers.indexOfFirst { it.first == change.id }
                    if (index < 0) continue
                    positions[change.id] = change.position
                    if (pointers.size == 1) {
                        dispatch(MotionEvent.ACTION_UP, change.uptimeMillis)
                    } else {
                        dispatch(MotionEvent.ACTION_POINTER_UP or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), change.uptimeMillis)
                    }
                    pointers.removeAt(index)
                    positions.remove(change.id)
                }
            }
            if (state.disallowIntercept) event.changes.forEach { it.consume() }
        }
    }
}
