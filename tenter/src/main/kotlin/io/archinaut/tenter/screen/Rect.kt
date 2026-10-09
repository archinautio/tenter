// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/** An immutable, nonnegative cell rectangle. Coordinates are local to its containing canvas. */
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

    /** True when ([x], [y]) is inside this rectangle's positive-area cells. */
    public fun contains(x: Int, y: Int): Boolean =
        x.toLong() >= this.x &&
            y.toLong() >= this.y &&
            x.toLong() < this.x.toLong() + width &&
            y.toLong() < this.y.toLong() + height

    /** Removes [padding], clamping each edge so the result remains a valid rectangle. */
    public fun inner(padding: Padding): Rect {
        val left = padding.left.coerceAtMost(width)
        val top = padding.top.coerceAtMost(height)
        val right = padding.right.coerceAtMost(width - left)
        val bottom = padding.bottom.coerceAtMost(height - top)
        return Rect(
            x = x + left,
            y = y + top,
            width = width - left - right,
            height = height - top - bottom,
        )
    }
}
