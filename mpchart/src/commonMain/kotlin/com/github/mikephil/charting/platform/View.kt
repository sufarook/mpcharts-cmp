package com.github.mikephil.charting.platform

import kotlin.math.abs
import kotlin.time.TimeSource

/** Work posted to a [View], like java.lang.Runnable. */
public fun interface Runnable {
    public fun run()
}

/** Monotonic clock in milliseconds, like android.view.animation.AnimationUtils. */
public object AnimationUtils {
    private val start = TimeSource.Monotonic.markNow()

    public fun currentAnimationTimeMillis(): Long = start.elapsedNow().inWholeMilliseconds
}

/** Stand-in for java.lang.System; [currentTimeMillis] is monotonic and only meant for measuring durations. */
public object System {
    public fun currentTimeMillis(): Long = AnimationUtils.currentAnimationTimeMillis()
}

/** Stand-in for the parts of java.lang.Math the charts use. */
public object Math {
    public const val PI: Double = kotlin.math.PI

    public fun toRadians(degrees: Double): Double = degrees / 180.0 * PI
    public fun toDegrees(radians: Double): Double = radians * 180.0 / PI
    public fun floorMod(x: Int, y: Int): Int = x.mod(y)
    public fun floorMod(x: Long, y: Long): Long = x.mod(y)
}

/** Logging, like android.util.Log. */
public object Log {
    public var isEnabled: Boolean = true

    public fun v(tag: String, msg: String): Int = print("V", tag, msg)
    public fun d(tag: String, msg: String): Int = print("D", tag, msg)
    public fun i(tag: String, msg: String): Int = print("I", tag, msg)
    public fun w(tag: String, msg: String): Int = print("W", tag, msg)
    public fun e(tag: String, msg: String): Int = print("E", tag, msg)

    private fun print(level: String, tag: String, msg: String): Int {
        if (isEnabled) println("$level/$tag: $msg")
        return 0
    }
}

/**
 * Android's view configuration constants in dp and milliseconds. The pixel values follow [density], which the
 * chart composables set from the screen density, so gestures trigger at the same physical distances everywhere.
 */
public class ViewConfiguration private constructor() {
    public val scaledTouchSlop: Int get() = (TOUCH_SLOP_DP * density + 0.5f).toInt()
    public val scaledDoubleTapSlop: Int get() = (DOUBLE_TAP_SLOP_DP * density + 0.5f).toInt()
    public val scaledMinimumFlingVelocity: Int get() = (MINIMUM_FLING_VELOCITY_DP * density + 0.5f).toInt()
    public val scaledMaximumFlingVelocity: Int get() = (MAXIMUM_FLING_VELOCITY_DP * density + 0.5f).toInt()

    public companion object {
        private const val TOUCH_SLOP_DP = 8f
        private const val DOUBLE_TAP_SLOP_DP = 100f
        private const val MINIMUM_FLING_VELOCITY_DP = 50f
        private const val MAXIMUM_FLING_VELOCITY_DP = 8000f

        /** Screen density, pixels per dp. */
        public var density: Float = 1f

        private val instance = ViewConfiguration()

        public fun get(@Suppress("UNUSED_PARAMETER") view: View?): ViewConfiguration = instance

        public fun getDoubleTapTimeout(): Int = 300
        public fun getDoubleTapMinTime(): Int = 40
        public fun getLongPressTimeout(): Int = 400
        public fun getTapTimeout(): Int = 100
    }
}

/** Receives requests from a [View]; implemented by the chart composables. */
public interface ViewHost {
    /** The view needs to be drawn again. */
    public fun invalidate()

    /** The view wants [View.onFrame] called on the next frame. */
    public fun requestFrame()

    /** While true, touch events go to the view and parents must not scroll. */
    public fun requestDisallowInterceptTouchEvent(disallow: Boolean)
}

/** The parent of a [View], reduced to what the charts call. */
public interface ViewParent {
    public fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean)
}

/**
 * Minimal stand-in for android.view.View: a size, invalidation, posted work, touch dispatch and drawing. A
 * composable hosts it, sizes it, feeds it touch events and draws it on every frame it is invalidated.
 */
public open class View {

    public var width: Int = 0
        private set
    public var height: Int = 0
        private set

    public var isEnabled: Boolean = true

    public var contentDescription: CharSequence? = null

    public var host: ViewHost? = null
        internal set

    private var frameRequested = false

    private class Task(val runnable: Runnable, val atMillis: Long)

    private val tasks = mutableListOf<Task>()

    private var onTouchListener: OnTouchListener? = null

    public val parent: ViewParent? = object : ViewParent {
        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            host?.requestDisallowInterceptTouchEvent(disallowIntercept)
        }
    }

    public open fun invalidate() {
        host?.invalidate()
    }

    public fun postInvalidate(): Unit = invalidate()

    /** Redraws on the next frame and calls [computeScroll] before drawing, like on Android. */
    public fun postInvalidateOnAnimation() {
        frameRequested = true
        host?.requestFrame()
    }

    public fun post(action: Runnable): Boolean = postDelayed(action, 0)

    public fun postDelayed(action: Runnable, delayMillis: Long): Boolean {
        tasks.add(Task(action, AnimationUtils.currentAnimationTimeMillis() + delayMillis))
        host?.requestFrame()
        return true
    }

    public fun removeCallbacks(action: Runnable): Boolean = tasks.removeAll { it.runnable === action }

    /** True while the view needs frames: posted work, a requested redraw or running animations. */
    public val needsFrames: Boolean
        get() = frameRequested || tasks.isNotEmpty() || AnimationClock.hasActive

    /** Runs due posted work and animations. Called by the host once per frame while [needsFrames]. */
    public fun onFrame() {
        val now = AnimationUtils.currentAnimationTimeMillis()
        if (tasks.isNotEmpty()) {
            val due = tasks.filter { it.atMillis <= now }
            tasks.removeAll(due)
            for (task in due) task.runnable.run()
        }
        AnimationClock.tick(now)
        if (frameRequested) {
            frameRequested = false
            invalidate()
        }
    }

    /** Sets the size, calling [onSizeChanged] when it changes. Called by the host on layout. */
    public fun setSize(width: Int, height: Int) {
        if (width == this.width && height == this.height) return
        val oldWidth = this.width
        val oldHeight = this.height
        this.width = width
        this.height = height
        onSizeChanged(width, height, oldWidth, oldHeight)
    }

    /** Drawn behind the content, filling the view, like View.getBackground(). */
    public var background: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }

    /** Sets a [ColorDrawable] of [color] as [background], like View.setBackgroundColor. */
    public fun setBackgroundColor(color: Int) {
        val current = background
        if (current is ColorDrawable) {
            current.color = color
            invalidate()
        } else {
            background = ColorDrawable(color)
        }
    }

    /** Draws the view: [computeScroll], the [background], then [onDraw], like Android's draw pass. */
    public fun draw(canvas: Canvas) {
        computeScroll()
        background?.let {
            it.setBounds(0, 0, width, height)
            it.draw(canvas)
        }
        onDraw(canvas)
    }

    public fun setOnTouchListener(listener: OnTouchListener?) {
        onTouchListener = listener
    }

    /** Delivers a touch event: the touch listener first, then [onTouchEvent], like View.dispatchTouchEvent. */
    public fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val listener = onTouchListener
        if (listener != null && isEnabled && listener.onTouch(this, event)) return true
        return onTouchEvent(event)
    }

    /** Attaches the view to [host]. */
    public fun attach(host: ViewHost) {
        this.host = host
        onAttachedToWindow()
    }

    /** Detaches the view from its host. */
    public fun detach() {
        onDetachedFromWindow()
        tasks.clear()
        frameRequested = false
        host = null
    }

    protected open fun onDraw(canvas: Canvas) {}
    protected open fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {}
    public open fun onTouchEvent(event: MotionEvent): Boolean = false
    public open fun computeScroll() {}
    protected open fun onAttachedToWindow() {}
    protected open fun onDetachedFromWindow() {}

    public fun interface OnTouchListener {
        public fun onTouch(v: View, event: MotionEvent): Boolean
    }
}

/** A touch event with all its pointers, like android.view.MotionEvent. Coordinates are in view pixels. */
public class MotionEvent private constructor(
    public val downTime: Long,
    public val eventTime: Long,
    public var action: Int,
    private val ids: IntArray,
    private val xs: FloatArray,
    private val ys: FloatArray,
) {
    public val actionMasked: Int get() = action and ACTION_MASK
    public val actionIndex: Int get() = (action and ACTION_POINTER_INDEX_MASK) shr ACTION_POINTER_INDEX_SHIFT
    public val pointerCount: Int get() = ids.size
    public val x: Float get() = xs[0]
    public val y: Float get() = ys[0]

    public fun getX(pointerIndex: Int): Float = xs[pointerIndex]
    public fun getY(pointerIndex: Int): Float = ys[pointerIndex]
    public fun getPointerId(pointerIndex: Int): Int = ids[pointerIndex]
    public fun findPointerIndex(pointerId: Int): Int = ids.indexOf(pointerId)

    public fun recycle() {}

    override fun toString(): String = "MotionEvent(action=$actionMasked, index=$actionIndex, pointers=${ids.indices.map { "${ids[it]}:${xs[it]},${ys[it]}" }})"

    public companion object {
        public const val ACTION_MASK: Int = 0xff
        public const val ACTION_DOWN: Int = 0
        public const val ACTION_UP: Int = 1
        public const val ACTION_MOVE: Int = 2
        public const val ACTION_CANCEL: Int = 3
        public const val ACTION_POINTER_DOWN: Int = 5
        public const val ACTION_POINTER_UP: Int = 6
        public const val ACTION_POINTER_INDEX_MASK: Int = 0xff00
        public const val ACTION_POINTER_INDEX_SHIFT: Int = 8

        /** Single pointer event. */
        public fun obtain(downTime: Long, eventTime: Long, action: Int, x: Float, y: Float, @Suppress("UNUSED_PARAMETER") metaState: Int): MotionEvent =
            MotionEvent(downTime, eventTime, action, intArrayOf(0), floatArrayOf(x), floatArrayOf(y))

        /** Event with several pointers, in pointer index order. */
        public fun obtain(downTime: Long, eventTime: Long, action: Int, pointerIds: IntArray, xs: FloatArray, ys: FloatArray): MotionEvent =
            MotionEvent(downTime, eventTime, action, pointerIds.copyOf(), xs.copyOf(), ys.copyOf())

        public fun obtain(other: MotionEvent): MotionEvent =
            MotionEvent(other.downTime, other.eventTime, other.action, other.ids.copyOf(), other.xs.copyOf(), other.ys.copyOf())
    }
}

/**
 * Pointer velocity from recent movement, like android.view.VelocityTracker with its default strategy: a
 * least squares fit of degree 2 over the samples of the last 100 ms, at most 20, cleared when the pointer
 * rested for 40 ms. Up events are not sampled, as on Android.
 */
public class VelocityTracker private constructor() {
    private class Sample(val time: Long, val x: Float, val y: Float)

    private val samples = mutableMapOf<Int, ArrayDeque<Sample>>()
    private val velocityX = mutableMapOf<Int, Float>()
    private val velocityY = mutableMapOf<Int, Float>()

    public fun clear() {
        samples.clear()
        velocityX.clear()
        velocityY.clear()
    }

    public fun recycle(): Unit = clear()

    public fun addMovement(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> return
            MotionEvent.ACTION_DOWN -> clear()
        }
        for (i in 0 until event.pointerCount) {
            val id = event.getPointerId(i)
            val list = samples.getOrPut(id) { ArrayDeque() }
            val last = list.lastOrNull()
            if (last != null && event.eventTime - last.time > ASSUME_POINTER_STOPPED_MILLIS) list.clear()
            list.addLast(Sample(event.eventTime, event.getX(i), event.getY(i)))
            while (list.size > HISTORY_SIZE) list.removeFirst()
        }
    }

    /** Computes velocities in pixels per [units] milliseconds, clamped to +-[maxVelocity]. */
    public fun computeCurrentVelocity(units: Int, maxVelocity: Float = Float.MAX_VALUE) {
        for ((id, list) in samples) {
            val newest = list.lastOrNull() ?: continue
            val recent = list.filter { newest.time - it.time <= HORIZON_MILLIS }
            val t = DoubleArray(recent.size) { (recent[it].time - newest.time) / 1000.0 }
            val vx = slope(t, DoubleArray(recent.size) { recent[it].x.toDouble() })
            val vy = slope(t, DoubleArray(recent.size) { recent[it].y.toDouble() })
            val scale = units / 1000f
            velocityX[id] = (vx.toFloat() * scale).coerceIn(-maxVelocity, maxVelocity)
            velocityY[id] = (vy.toFloat() * scale).coerceIn(-maxVelocity, maxVelocity)
        }
    }

    public fun getXVelocity(pointerId: Int = 0): Float = velocityX[pointerId] ?: 0f
    public fun getYVelocity(pointerId: Int = 0): Float = velocityY[pointerId] ?: 0f

    public companion object {
        private const val HISTORY_SIZE = 20
        private const val HORIZON_MILLIS = 100L
        private const val ASSUME_POINTER_STOPPED_MILLIS = 40L

        public fun obtain(): VelocityTracker = VelocityTracker()

        /** Derivative at t = 0 of the least squares polynomial (degree 2, or less with fewer samples). */
        private fun slope(t: DoubleArray, p: DoubleArray): Double {
            val n = t.size
            if (n < 2) return 0.0
            val degree = minOf(2, n - 1)
            val size = degree + 1
            // normal equations A^T A c = A^T p
            val ata = Array(size) { DoubleArray(size) }
            val atp = DoubleArray(size)
            for (k in 0 until n) {
                val powers = DoubleArray(size)
                powers[0] = 1.0
                for (j in 1 until size) powers[j] = powers[j - 1] * t[k]
                for (r in 0 until size) {
                    atp[r] += powers[r] * p[k]
                    for (c in 0 until size) ata[r][c] += powers[r] * powers[c]
                }
            }
            val coefficients = solve(ata, atp) ?: return 0.0
            return coefficients[1]
        }

        private fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
            val n = b.size
            val m = Array(n) { r -> DoubleArray(n + 1) { c -> if (c < n) a[r][c] else b[r] } }
            for (col in 0 until n) {
                var pivot = col
                for (r in col + 1 until n) if (abs(m[r][col]) > abs(m[pivot][col])) pivot = r
                if (abs(m[pivot][col]) < 1e-12) return null
                val tmp = m[col]
                m[col] = m[pivot]
                m[pivot] = tmp
                for (r in 0 until n) {
                    if (r == col) continue
                    val f = m[r][col] / m[col][col]
                    for (c in col..n) m[r][c] -= f * m[col][c]
                }
            }
            return DoubleArray(n) { m[it][n] / m[it][it] }
        }
    }
}

/**
 * Detects taps, double taps, long presses, scrolls and flings, a port of android.view.GestureDetector with the
 * same timeouts and slops. Timers run on the [view]'s posted work.
 */
public class GestureDetector(private val view: View, private val listener: OnGestureListener) {

    public interface OnGestureListener {
        public fun onDown(e: MotionEvent): Boolean
        public fun onShowPress(e: MotionEvent)
        public fun onSingleTapUp(e: MotionEvent): Boolean
        public fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean
        public fun onLongPress(e: MotionEvent)
        public fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean
    }

    public interface OnDoubleTapListener {
        public fun onSingleTapConfirmed(e: MotionEvent): Boolean
        public fun onDoubleTap(e: MotionEvent): Boolean
        public fun onDoubleTapEvent(e: MotionEvent): Boolean
    }

    public open class SimpleOnGestureListener : OnGestureListener, OnDoubleTapListener {
        override fun onDown(e: MotionEvent): Boolean = false
        override fun onShowPress(e: MotionEvent) {}
        override fun onSingleTapUp(e: MotionEvent): Boolean = false
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
        override fun onLongPress(e: MotionEvent) {}
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean = false
        override fun onDoubleTap(e: MotionEvent): Boolean = false
        override fun onDoubleTapEvent(e: MotionEvent): Boolean = false
    }

    private val doubleTapListener = listener as? OnDoubleTapListener

    public var isLongpressEnabled: Boolean = true

    private var currentDownEvent: MotionEvent? = null
    private var previousUpEvent: MotionEvent? = null
    private var stillDown = false
    private var deferConfirmSingleTap = false
    private var inLongPress = false
    private var alwaysInTapRegion = false
    private var alwaysInBiggerTapRegion = false
    private var isDoubleTapping = false
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var downFocusX = 0f
    private var downFocusY = 0f
    private var velocityTracker: VelocityTracker? = null

    private var tapPending = false
    private val tapMessage = Runnable {
        tapPending = false
        val down = currentDownEvent
        if (doubleTapListener != null && down != null) {
            if (!stillDown) doubleTapListener.onSingleTapConfirmed(down) else deferConfirmSingleTap = true
        }
    }
    private val longPressMessage = Runnable { dispatchLongPress() }
    private val showPressMessage = Runnable { currentDownEvent?.let { listener.onShowPress(it) } }

    private val config get() = ViewConfiguration.get(view)
    private val touchSlopSquare get() = config.scaledTouchSlop * config.scaledTouchSlop
    private val doubleTapSlopSquare get() = config.scaledDoubleTapSlop * config.scaledDoubleTapSlop

    public fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        val tracker = velocityTracker ?: VelocityTracker.obtain().also { velocityTracker = it }
        tracker.addMovement(ev)

        val pointerUp = action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_POINTER_UP
        val skipIndex = if (pointerUp) ev.actionIndex else -1

        var sumX = 0f
        var sumY = 0f
        for (i in 0 until ev.pointerCount) {
            if (skipIndex == i) continue
            sumX += ev.getX(i)
            sumY += ev.getY(i)
        }
        val div = if (pointerUp) ev.pointerCount - 1 else ev.pointerCount
        val focusX = sumX / div
        val focusY = sumY / div

        var handled = false

        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                downFocusX = focusX
                lastFocusX = focusX
                downFocusY = focusY
                lastFocusY = focusY
                cancelTaps()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                downFocusX = focusX
                lastFocusX = focusX
                downFocusY = focusY
                lastFocusY = focusY
                tracker.computeCurrentVelocity(1000, config.scaledMaximumFlingVelocity.toFloat())
                val upIndex = ev.actionIndex
                val id1 = ev.getPointerId(upIndex)
                val x1 = tracker.getXVelocity(id1)
                val y1 = tracker.getYVelocity(id1)
                for (i in 0 until ev.pointerCount) {
                    if (i == upIndex) continue
                    val id2 = ev.getPointerId(i)
                    val dot = x1 * tracker.getXVelocity(id2) + y1 * tracker.getYVelocity(id2)
                    if (dot < 0) {
                        tracker.clear()
                        break
                    }
                }
            }

            MotionEvent.ACTION_DOWN -> {
                if (doubleTapListener != null) {
                    val hadTapMessage = tapPending
                    if (hadTapMessage) removeTap()
                    val down = currentDownEvent
                    val up = previousUpEvent
                    if (down != null && up != null && hadTapMessage && isConsideredDoubleTap(down, up, ev)) {
                        isDoubleTapping = true
                        handled = handled or doubleTapListener.onDoubleTap(down)
                        handled = handled or doubleTapListener.onDoubleTapEvent(ev)
                    } else {
                        tapPending = true
                        view.postDelayed(tapMessage, ViewConfiguration.getDoubleTapTimeout().toLong())
                    }
                }
                downFocusX = focusX
                lastFocusX = focusX
                downFocusY = focusY
                lastFocusY = focusY
                currentDownEvent = MotionEvent.obtain(ev)
                alwaysInTapRegion = true
                alwaysInBiggerTapRegion = true
                stillDown = true
                inLongPress = false
                deferConfirmSingleTap = false
                if (isLongpressEnabled) {
                    view.removeCallbacks(longPressMessage)
                    view.postDelayed(longPressMessage, ViewConfiguration.getLongPressTimeout().toLong())
                }
                view.postDelayed(showPressMessage, ViewConfiguration.getTapTimeout().toLong())
                handled = handled or listener.onDown(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!inLongPress) {
                    val scrollX = lastFocusX - focusX
                    val scrollY = lastFocusY - focusY
                    if (isDoubleTapping) {
                        handled = handled or (doubleTapListener?.onDoubleTapEvent(ev) ?: false)
                    } else if (alwaysInTapRegion) {
                        val deltaX = (focusX - downFocusX).toInt()
                        val deltaY = (focusY - downFocusY).toInt()
                        val distance = deltaX * deltaX + deltaY * deltaY
                        if (distance > touchSlopSquare) {
                            handled = listener.onScroll(currentDownEvent, ev, scrollX, scrollY)
                            lastFocusX = focusX
                            lastFocusY = focusY
                            alwaysInTapRegion = false
                            removeTap()
                            view.removeCallbacks(showPressMessage)
                            view.removeCallbacks(longPressMessage)
                        }
                        if (distance > touchSlopSquare) alwaysInBiggerTapRegion = false
                    } else if (abs(scrollX) >= 1 || abs(scrollY) >= 1) {
                        handled = listener.onScroll(currentDownEvent, ev, scrollX, scrollY)
                        lastFocusX = focusX
                        lastFocusY = focusY
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                stillDown = false
                val currentUpEvent = MotionEvent.obtain(ev)
                if (isDoubleTapping) {
                    handled = handled or (doubleTapListener?.onDoubleTapEvent(ev) ?: false)
                } else if (inLongPress) {
                    removeTap()
                    inLongPress = false
                } else if (alwaysInTapRegion) {
                    handled = listener.onSingleTapUp(ev)
                    if (deferConfirmSingleTap && doubleTapListener != null) doubleTapListener.onSingleTapConfirmed(ev)
                } else {
                    val pointerId = ev.getPointerId(0)
                    tracker.computeCurrentVelocity(1000, config.scaledMaximumFlingVelocity.toFloat())
                    val velocityY = tracker.getYVelocity(pointerId)
                    val velocityX = tracker.getXVelocity(pointerId)
                    val minimum = config.scaledMinimumFlingVelocity
                    if (abs(velocityY) > minimum || abs(velocityX) > minimum) {
                        handled = listener.onFling(currentDownEvent, ev, velocityX, velocityY)
                    }
                }
                previousUpEvent = currentUpEvent
                velocityTracker = null
                isDoubleTapping = false
                deferConfirmSingleTap = false
                view.removeCallbacks(showPressMessage)
                view.removeCallbacks(longPressMessage)
            }

            MotionEvent.ACTION_CANCEL -> cancel()
        }
        return handled
    }

    private fun removeTap() {
        tapPending = false
        view.removeCallbacks(tapMessage)
    }

    private fun cancel() {
        view.removeCallbacks(showPressMessage)
        view.removeCallbacks(longPressMessage)
        removeTap()
        velocityTracker = null
        isDoubleTapping = false
        stillDown = false
        alwaysInTapRegion = false
        alwaysInBiggerTapRegion = false
        deferConfirmSingleTap = false
        inLongPress = false
    }

    private fun cancelTaps() {
        view.removeCallbacks(showPressMessage)
        view.removeCallbacks(longPressMessage)
        removeTap()
        isDoubleTapping = false
        alwaysInTapRegion = false
        alwaysInBiggerTapRegion = false
        deferConfirmSingleTap = false
        inLongPress = false
    }

    private fun dispatchLongPress() {
        removeTap()
        deferConfirmSingleTap = false
        inLongPress = true
        currentDownEvent?.let { listener.onLongPress(it) }
    }

    private fun isConsideredDoubleTap(firstDown: MotionEvent, firstUp: MotionEvent, secondDown: MotionEvent): Boolean {
        if (!alwaysInBiggerTapRegion) return false
        val deltaTime = secondDown.eventTime - firstUp.eventTime
        if (deltaTime > ViewConfiguration.getDoubleTapTimeout() || deltaTime < ViewConfiguration.getDoubleTapMinTime()) return false
        val deltaX = firstDown.x.toInt() - secondDown.x.toInt()
        val deltaY = firstDown.y.toInt() - secondDown.y.toInt()
        return deltaX * deltaX + deltaY * deltaY < doubleTapSlopSquare
    }
}

/** Stand-in for java.lang.ref.WeakReference that holds its referent until [clear]. */
public class WeakReference<T : Any>(referent: T?) {
    private var value: T? = referent

    public fun get(): T? = value

    public fun clear() {
        value = null
    }
}
