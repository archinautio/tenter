// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/** An immutable, nonnegative cell rectangle. */
public data class Rect(
    public val x: Int,
    public val y: Int,
    public val width: Int,
    public val height: Int,
) {
    init {
        require(width >= 0) { "rectangle width must not be negative: $width" }
        require(height >= 0) { "rectangle height must not be negative: $height" }
    }

    /** True when this rectangle covers no cells. */
    public val isEmpty: Boolean get() = width == 0 || height == 0

    /** True when ([x], [y]) is inside this rectangle's positive-area cells. */
    public fun contains(x: Int, y: Int): Boolean =
        x >= this.x &&
            y >= this.y &&
            x < this.x + width &&
            y < this.y + height

    /**
     * The part of this rectangle inside [bounds]. When they don't overlap, the result is empty and
     * sits on the nearest edge of [bounds], so it is always contained in [bounds].
     */
    internal fun clipTo(bounds: Rect): Rect {
        val boundsMaxX = bounds.x + bounds.width
        val boundsMaxY = bounds.y + bounds.height

        val right = x + width
        val bottom = y + height

        // Fast-path: already entirely inside bounds
        if (x >= bounds.x && y >= bounds.y && right <= boundsMaxX && bottom <= boundsMaxY) {
            return this
        }

        val left = maxOf(bounds.x, minOf(x, boundsMaxX))
        val top = maxOf(bounds.y, minOf(y, boundsMaxY))
        val clampedRight = maxOf(bounds.x, minOf(right, boundsMaxX))
        val clampedBottom = maxOf(bounds.y, minOf(bottom, boundsMaxY))

        return Rect(
            x = left,
            y = top,
            width = clampedRight - left,
            height = clampedBottom - top,
        )
    }

    /** This rectangle moved by ([dx], [dy]). */
    internal fun offset(dx: Int, dy: Int): Rect = copy(x = x + dx, y = y + dy)

    /** Removes [padding], clamping each edge so the result remains a valid rectangle. */
    public fun inner(padding: Padding): Rect {
        if (padding.isZero()) return this // Fast-path: no padding applied

        val left = minOf(padding.left, width)
        val top = minOf(padding.top, height)

        return Rect(
            x = x + left,
            y = y + top,
            width = maxOf(0, width - left - padding.right),
            height = maxOf(0, height - top - padding.bottom),
        )
    }
}
