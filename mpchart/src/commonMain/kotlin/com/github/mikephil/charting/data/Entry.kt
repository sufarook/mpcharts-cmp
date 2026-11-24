package com.github.mikephil.charting.data

import com.github.mikephil.charting.platform.Drawable
import com.github.mikephil.charting.utils.Utils
import kotlin.math.abs

/**
 * One point in a chart: an x value, a y value, an optional icon and a typed payload.
 * Used directly by line and scatter charts; the other chart types use subclasses.
 * Use the [Entry] factory function when there is no payload; it returns an `Entry<Nothing>`.
 *
 * @param D the type of the payload stored in [data].
 * @property x Position on the x axis in value space (not an index).
 */
public open class Entry<out D>(
    public var x: Float,
    y: Float,
    icon: Drawable? = null,
    data: D?
) : BaseEntry<D>(y, icon, data) {

    /** Returns a new entry with the same x, y, icon and payload. The icon and payload are shared, not copied. */
    public open fun copy(): Entry<D> = Entry(x, y, icon, data)

    /**
     * Compares x and y within [Utils.FLOAT_EPSILON] and requires the same payload instance.
     * Unlike `equals`, two separately created entries with the same values are considered equal.
     *
     * @return true if [e] is not null and matches in x, y and payload.
     */
    public fun equalTo(e: Entry<*>?): Boolean {
        if (e == null) return false
        if (e.data !== data) return false
        if (abs(e.x - x) > Utils.FLOAT_EPSILON) return false
        if (abs(e.y - y) > Utils.FLOAT_EPSILON) return false
        return true
    }

    override fun toString(): String = "Entry, x: $x y: $y"

}

/**
 * Creates an entry without a payload.
 *
 * @param x position on the x axis in value space.
 * @param y the y value.
 * @param icon drawable drawn at the entry when icons are enabled, or null.
 */
public fun Entry(x: Float = 0f, y: Float = 0f, icon: Drawable? = null): Entry<Nothing> = Entry(x, y, icon, null)
