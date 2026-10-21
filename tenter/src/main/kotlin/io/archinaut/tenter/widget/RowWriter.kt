// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.text.TextTruncation

/**
 * Writes a [MeasurableWidget]'s content row by row. [width] is the available width and row 0 is
 * the widget's top; the widget's height is the furthest row written. Lines are ellipsized to the
 * width, so a caller never computes where text must stop.
 */
public class RowWriter internal constructor(
    private val sink: RecordingTextSink,
) {

    public val width: Int get() = sink.width

    /** The row the next [writeLine] or [writeRow] lands on. */
    public var row: Int = 0
        private set

    internal var occupiedHeight: Int = 0
        private set

    /**
     * Writes [content] in its own area starting at the current row, clipped to its measured size,
     * and advances past it. This is how a measurable widget places a child; returns the rows used.
     * A child measured wider than [width] widens this widget too, as it would a [Stack], so an
     * enclosing [Viewport] can scroll to it.
     */
    public fun draw(content: MeasurableWidget): Int {
        val layout = content.prepare(width)
        sink.place(layout, 0, row)
        repeat(layout.height) { newLine() }
        return layout.height
    }

    /** Writes [text] on the current row, ellipsized to [width], and advances. Returns the row written. */
    public fun writeLine(text: String, style: Style = Style.DEFAULT): Int = writeLine(Line(text, style))

    /** Writes [line] on the current row, ellipsized to [width], each span in its own style, and advances. */
    public fun writeLine(line: Line): Int {
        val written = row
        var column = 0
        for (span in line.ellipsize(width).spans) {
            sink.write(column, written, span.text, span.style)
            column += CellWidth.of(span.text)
        }
        row += 1
        occupy(row)
        return written
    }

    /**
     * Writes [left] flush to the left edge (truncated with an ellipsis if it would collide with
     * [right]) and [right] flush to the right edge, then advances. [rightStyle] defaults to
     * [leftStyle] for a single-color row. Returns the row written.
     */
    public fun writeRow(left: String, right: String, leftStyle: Style = Style.DEFAULT, rightStyle: Style = leftStyle): Int {
        val visibleRight = TextTruncation.ellipsize(right, width)
        val rightWidth = CellWidth.of(visibleRight)
        val maxLeft = (width - rightWidth - 1).coerceAtLeast(0)
        val written = row
        sink.write(0, written, TextTruncation.ellipsize(left, maxLeft), leftStyle)
        sink.write(width - rightWidth, written, visibleRight, rightStyle)
        row += 1
        occupy(row)
        return written
    }

    /**
     * Writes [text] at [column] on [row] — the current one unless an earlier row is being overlaid —
     * without advancing. The text is not ellipsized; anything past the widget's edge is clipped.
     */
    public fun write(column: Int, text: String, style: Style = Style.DEFAULT, row: Int = this.row) {
        sink.write(column, row, text, style)
        occupy(row + 1)
    }

    /** Advances to the next row, leaving the current one blank if nothing was written to it. */
    public fun newLine() {
        row += 1
        occupy(row)
    }

    /** Marks [height] full-width rows from [row] as the content an enclosing viewport should keep visible. */
    public fun markReveal(row: Int = this.row, height: Int = 1) {
        sink.reveal(Rect(0, row, width, height))
    }

    private fun occupy(height: Int) {
        if (height > occupiedHeight) occupiedHeight = height
    }
}
