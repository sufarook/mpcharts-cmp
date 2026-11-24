package com.github.mikephil.charting.platform

/** Maps elapsed fraction to animated fraction, like android.animation.TimeInterpolator. */
public fun interface TimeInterpolator {
    public fun getInterpolation(input: Float): Float
}

/**
 * Advances every running [ValueAnimator]. Chart hosts tick it once per frame; hosts register a waker so a
 * starting animation gets them frames.
 */
public object AnimationClock {
    private val active = mutableListOf<ValueAnimator>()
    private val wakers = mutableListOf<() -> Unit>()

    public val hasActive: Boolean get() = active.isNotEmpty()

    public fun addWaker(waker: () -> Unit) {
        wakers.add(waker)
    }

    public fun removeWaker(waker: () -> Unit) {
        wakers.remove(waker)
    }

    internal fun add(animator: ValueAnimator) {
        if (animator !in active) active.add(animator)
        for (w in wakers.toList()) w()
    }

    internal fun remove(animator: ValueAnimator) {
        active.remove(animator)
    }

    /** Advances all running animators to [nowMillis]. Ticks at the same time are ignored. */
    public fun tick(nowMillis: Long) {
        if (nowMillis == lastTick) return
        lastTick = nowMillis
        for (animator in active.toList()) animator.doFrame(nowMillis)
    }

    private var lastTick = -1L
}

/** Base class of animators, like android.animation.Animator. */
public abstract class Animator {
    public interface AnimatorListener {
        public fun onAnimationStart(animation: Animator)
        public fun onAnimationEnd(animation: Animator)
        public fun onAnimationCancel(animation: Animator)
        public fun onAnimationRepeat(animation: Animator)
    }

    protected val listeners: MutableList<AnimatorListener> = mutableListOf()

    public fun addListener(listener: AnimatorListener) {
        listeners.add(listener)
    }

    public fun removeListener(listener: AnimatorListener) {
        listeners.remove(listener)
    }

    public fun removeAllListeners() {
        listeners.clear()
    }

    public abstract var duration: Long
    public abstract var interpolator: TimeInterpolator?
    public abstract val isRunning: Boolean
    public abstract fun start()
    public abstract fun cancel()
    public abstract fun end()
}

/** Listener with empty defaults, like android.animation.AnimatorListenerAdapter. */
public abstract class AnimatorListenerAdapter : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator) {}
    override fun onAnimationEnd(animation: Animator) {}
    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
}

/**
 * Animates a float between values, like android.animation.ValueAnimator: [start] reports the first value at
 * once, every frame reports the interpolated value, [end] jumps to the last value and [cancel] stops where it is.
 */
public class ValueAnimator private constructor(private val values: FloatArray) : Animator() {

    public fun interface AnimatorUpdateListener {
        public fun onAnimationUpdate(animation: ValueAnimator)
    }

    private val updateListeners = mutableListOf<AnimatorUpdateListener>()

    override var duration: Long = 300
    override var interpolator: TimeInterpolator? = null

    private var running = false
    private var startTime = -1L
    private var reversed = false

    /** Plays the animation backwards from where it is, or from the end when it is not running. */
    public fun reverse() {
        if (running) {
            val now = AnimationUtils.currentAnimationTimeMillis()
            val elapsed = (now - startTime).coerceIn(0, duration)
            startTime = now - (duration - elapsed)
            reversed = !reversed
        } else {
            reversed = !reversed
            start()
        }
    }

    override val isRunning: Boolean get() = running

    public var animatedFraction: Float = 0f
        private set

    /** The current value, a Float. */
    public var animatedValue: Any = values.first()
        private set

    public fun addUpdateListener(listener: AnimatorUpdateListener) {
        updateListeners.add(listener)
    }

    public fun removeUpdateListener(listener: AnimatorUpdateListener) {
        updateListeners.remove(listener)
    }

    public fun removeAllUpdateListeners() {
        updateListeners.clear()
    }

    override fun start() {
        if (running) cancel()
        running = true
        startTime = AnimationUtils.currentAnimationTimeMillis()
        for (l in listeners.toList()) l.onAnimationStart(this)
        animateValue(if (reversed) 1f else 0f)
        AnimationClock.add(this)
    }

    internal fun doFrame(now: Long) {
        if (!running) return
        val fraction = if (duration <= 0) 1f else ((now - startTime).toFloat() / duration).coerceIn(0f, 1f)
        animateValue(if (reversed) 1f - fraction else fraction)
        if (fraction >= 1f) endAnimation()
    }

    override fun cancel() {
        if (!running) return
        for (l in listeners.toList()) l.onAnimationCancel(this)
        endAnimation()
    }

    override fun end() {
        if (!running) {
            running = true
            for (l in listeners.toList()) l.onAnimationStart(this)
        }
        animateValue(if (reversed) 0f else 1f)
        endAnimation()
    }

    private fun animateValue(fraction: Float) {
        val eased = interpolator?.getInterpolation(fraction) ?: fraction
        animatedFraction = eased
        animatedValue = valueAt(eased)
        for (l in updateListeners.toList()) l.onAnimationUpdate(this)
    }

    private fun valueAt(fraction: Float): Float {
        if (values.size == 1) return values[0]
        val segments = values.size - 1
        val position = (fraction * segments).coerceIn(0f, segments.toFloat())
        val index = position.toInt().coerceAtMost(segments - 1)
        val local = position - index
        return values[index] + (values[index + 1] - values[index]) * local
    }

    private fun endAnimation() {
        AnimationClock.remove(this)
        running = false
        for (l in listeners.toList()) l.onAnimationEnd(this)
    }

    public companion object {
        public fun ofFloat(vararg values: Float): ValueAnimator = ValueAnimator(values.copyOf())
    }
}
