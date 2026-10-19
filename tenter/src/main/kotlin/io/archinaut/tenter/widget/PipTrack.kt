// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.CellWidth

/**
 * A "used-of-capacity" pip track: [used] pips drawn with [filled], the remaining
 * `capacity - used` drawn with [empty], wrapping to a new row every [perRow] pips. This is the
 * shared primitive behind any discrete-dot render of a bounded resource (health tracks, ammo
 * counters, capacity gauges) — those differ only in glyphs, capacity, and how many pips fit per
 * row, not in the wrapping/coloring logic itself.
 *
 * Its height is the number of rows [capacity] pips occupy, at least one. Place several tracks
 * side by side with [Columns], or stack them with [Stack] or [TextCursor.draw].
 */
public class PipTrack(
    private val used: Int,
    private val capacity: Int,
    private val filled: String,
    private val empty: String,
    private val perRow: Int,
    private val usedStyle: Style,
    private val emptyStyle: Style,
    private val spacing: Int = 1,
) : MeasurableWidget {
    init {
        require(perRow > 0) { "perRow must be positive, was $perRow" }
        require(spacing >= 0) { "spacing must not be negative, was $spacing" }
    }

    override fun write(cursor: TextCursor) {
        val filledCount = used.coerceIn(0, capacity)
        val stride = maxOf(CellWidth.of(filled), CellWidth.of(empty)) + spacing

        for (i in 0 until capacity) {
            val row = i / perRow
            val column = (i % perRow) * stride
            if (i < filledCount) {
                cursor.writeAt(column, row, filled, usedStyle)
            } else {
                cursor.writeAt(column, row, empty, emptyStyle)
            }
        }
        repeat(((capacity - 1).coerceAtLeast(0) / perRow) + 1) { cursor.newLine() }
    }
}
