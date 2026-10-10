// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/** Space removed from each edge of a [Rect] or [Canvas] to derive a smaller inner area. */
public data class Padding(
    public val left: Int = 0,
    public val top: Int = 0,
    public val right: Int = 0,
    public val bottom: Int = 0,
) {
    init {
        require(left >= 0) { "left padding must not be negative: $left" }
        require(top >= 0) { "top padding must not be negative: $top" }
        require(right >= 0) { "right padding must not be negative: $right" }
        require(bottom >= 0) { "bottom padding must not be negative: $bottom" }
    }

    public operator fun plus(other: Padding): Padding = Padding(
        left + other.left,
        top + other.top,
        right + other.right,
        bottom + other.bottom,
    )

    /** This padding with the top and bottom edges dropped. */
    public fun horizontalEdges(): Padding = Padding(left = left, right = right)

    /** This padding with the left and right edges dropped. */
    public fun verticalEdges(): Padding = Padding(top = top, bottom = bottom)

    /** True when all edges are zero. */
    public fun isZero(): Boolean = this === ZERO || (left or top or right or bottom) == 0

    public companion object {
        public val ZERO: Padding = Padding()

        /** [n] cells on every edge. */
        public fun uniform(n: Int): Padding = Padding(n, n, n, n)

        /** [n] cells on the left and right edges. */
        public fun horizontal(n: Int): Padding = Padding(left = n, right = n)

        /** [n] cells on the top and bottom edges. */
        public fun vertical(n: Int): Padding = Padding(top = n, bottom = n)
    }
}
