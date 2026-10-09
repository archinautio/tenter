// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/** The axis a [Layout] splits along. */
public enum class Direction {
    /** Segments sit side by side, left to right. */
    HORIZONTAL,

    /** Segments are stacked, top to bottom. */
    VERTICAL,
}

/**
 * Splits a [Rect] into adjacent segments along one axis, in the manner of Ratatui's `Layout`:
 *
 * ```kotlin
 * val (header, body, footer) = Layout.vertical(Length(1), Fill(1), Length(1)).split(frame.area)
 * ```
 *
 * Resolution is integer-only and deterministic. After removing [margin] and the [spacing]
 * between segments, each segment first takes its requested size: [Constraint.Length] and
 * [Constraint.Min] their value, [Constraint.Max] its maximum, [Constraint.Percentage] and
 * [Constraint.Ratio] their share of the available cells rounded down, and [Constraint.Fill]
 * nothing. Leftover cells then go to fill segments in proportion to their weights; without a
 * weighted fill segment they go to min segments equally; otherwise they stay empty after the last
 * segment. Integer remainders go to the earliest receiving segments. When the requests exceed the
 * space, segments other than min segments shrink first, from the last segment backwards, and then
 * min segments do the same. Every returned rectangle lies inside the split area.
 */
public class Layout private constructor(
    public val direction: Direction,
    public val constraints: List<Constraint>,
    public val margin: Padding,
    public val spacing: Int,
) {
    init {
        require(spacing >= 0) { "spacing must not be negative: $spacing" }
    }

    /** This layout with [margin] removed from the split area first. */
    public fun margin(margin: Padding): Layout = Layout(direction, constraints, margin, spacing)

    /** This layout with [spacing] empty cells between adjacent segments. */
    public fun spacing(spacing: Int): Layout = Layout(direction, constraints, margin, spacing)

    /** One rectangle per constraint, in order. */
    public fun split(area: Rect): List<Rect> {
        if (constraints.isEmpty()) return emptyList()
        val inner = area.inner(margin)
        val horizontal = direction == Direction.HORIZONTAL
        val start = if (horizontal) inner.x else inner.y
        val total = if (horizontal) inner.width else inner.height
        val gaps = spacing.toLong() * (constraints.size - 1)
        val available = (total - gaps).coerceAtLeast(0L)
        val sizes = resolve(available)

        var offset = start.toLong()
        val end = start.toLong() + total
        return sizes.map { size ->
            val position = offset.coerceAtMost(end).toInt()
            offset += size + spacing
            if (horizontal) {
                Rect(position, inner.y, size.toInt(), inner.height)
            } else {
                Rect(inner.x, position, inner.width, size.toInt())
            }
        }
    }

    private fun resolve(available: Long): LongArray {
        val sizes = LongArray(constraints.size) { index ->
            when (val constraint = constraints[index]) {
                is Constraint.Length -> constraint.length.toLong()
                is Constraint.Min -> constraint.min.toLong()
                is Constraint.Max -> constraint.max.toLong()
                is Constraint.Percentage -> available * constraint.percent / 100
                is Constraint.Ratio -> available * constraint.numerator / constraint.denominator
                is Constraint.Fill -> 0L
            }
        }
        val requested = sizes.sum()
        if (requested < available) {
            grow(sizes, available - requested)
        } else if (requested > available) {
            shrink(sizes, requested - available)
        }
        return sizes
    }

    private fun grow(sizes: LongArray, excess: Long) {
        val fills = constraints.indices.filter { (constraints[it] as? Constraint.Fill)?.weight?.let { w -> w > 0 } == true }
        if (fills.isNotEmpty()) {
            distribute(sizes, excess, fills) { (constraints[it] as Constraint.Fill).weight.toLong() }
            return
        }
        val mins = constraints.indices.filter { constraints[it] is Constraint.Min }
        if (mins.isNotEmpty()) distribute(sizes, excess, mins) { 1L }
    }

    private fun distribute(sizes: LongArray, amount: Long, receivers: List<Int>, weight: (Int) -> Long) {
        val totalWeight = receivers.sumOf(weight)
        var given = 0L
        for (index in receivers) {
            val share = amount * weight(index) / totalWeight
            sizes[index] += share
            given += share
        }
        var remainder = amount - given
        for (index in receivers) {
            if (remainder == 0L) break
            sizes[index]++
            remainder--
        }
    }

    private fun shrink(sizes: LongArray, deficit: Long) {
        var remaining = deficit
        val order = sizes.indices.reversed().partition { constraints[it] !is Constraint.Min }
        for (index in order.first + order.second) {
            if (remaining == 0L) return
            val taken = minOf(sizes[index], remaining)
            sizes[index] -= taken
            remaining -= taken
        }
    }

    public companion object {
        /** Segments stacked top to bottom. */
        public fun vertical(vararg constraints: Constraint): Layout = vertical(constraints.toList())

        /** Segments stacked top to bottom. */
        public fun vertical(constraints: List<Constraint>): Layout =
            Layout(Direction.VERTICAL, constraints.toList(), Padding.ZERO, 0)

        /** Segments side by side, left to right. */
        public fun horizontal(vararg constraints: Constraint): Layout = horizontal(constraints.toList())

        /** Segments side by side, left to right. */
        public fun horizontal(constraints: List<Constraint>): Layout =
            Layout(Direction.HORIZONTAL, constraints.toList(), Padding.ZERO, 0)
    }
}
