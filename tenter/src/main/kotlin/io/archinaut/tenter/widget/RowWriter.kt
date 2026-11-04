// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import com.github.ajalt.mordant.rendering.TextAlign
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.offset
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.text.TextTruncation

/** Writes a [MeasurableWidget]'s content row by row within [width]; its height is the furthest row written. */
public class RowWriter internal constructor(
    public val width: Int,
) {
    private val builder = ContentLayout.Builder()

    /** The row the next [writeLine] or [writeRow] lands on. */
    public var row: Int = 0
        private set

    private var occupiedWidth: Int = width

    private var occupiedHeight: Int = 0

    /** Writes [content] in its own area from the current row and advances past it; a wider child widens this widget too. */
    public fun draw(content: MeasurableWidget) {
        val layout = content.prepare(width)
        builder.place(0, row, layout)
        occupiedWidth = maxOf(occupiedWidth, layout.width)
        row = checkedAdd(row, layout.height, "written height")
        occupy(row)
    }

    /** Paints a plain [widget] across [width] and [height] rows from the current row, and advances past it. */
    public fun draw(widget: Widget, height: Int) {
        draw(widget.fixedSize(width, height))
    }

    /** Writes [text] as [writeLine] writes a [Line] in [style]. */
    public fun writeLine(text: String, style: Style = Style.DEFAULT, align: TextAlign = TextAlign.LEFT): Point =
        writeLine(Line(text, style), align)

    /**
     * Writes [line] on the current row, ellipsized to [width] and placed by [align], advances, and
     * returns the point after it. [TextAlign.JUSTIFY] widens the gaps between words to fill the row,
     * and [TextAlign.NONE] writes flush left. A line that does not fit fills the row whatever [align].
     */
    public fun writeLine(line: Line, align: TextAlign = TextAlign.LEFT): Point {
        val written = row
        val visible = line.ellipsize(width).let { if (align == TextAlign.JUSTIFY) it.justify(width) else it }
        var column = align.offset(visible.width, width)
        for (span in visible.spans) {
            builder.text(column, written, span.text, span.style)
            column += CellWidth.of(span.text)
        }
        row += 1
        occupy(row)
        return Point(column, written)
    }

    /** Writes [left] flush left, ellipsized before it reaches [right], and [right] flush right, then advances. */
    public fun writeRow(left: String, right: String, leftStyle: Style = Style.DEFAULT, rightStyle: Style = leftStyle) {
        val visibleRight = TextTruncation.ellipsize(right, width)
        val rightWidth = CellWidth.of(visibleRight)
        val maxLeft = (width - rightWidth - 1).coerceAtLeast(0)
        val written = row
        builder.text(0, written, TextTruncation.ellipsize(left, maxLeft), leftStyle)
        builder.text(width - rightWidth, written, visibleRight, rightStyle)
        row += 1
        occupy(row)
    }

    /** Writes [text] at [column] on [row], without advancing or ellipsizing, and returns the point after it. */
    public fun write(column: Int, text: String, style: Style = Style.DEFAULT, row: Int = this.row): Point {
        builder.text(column, row, text, style)
        occupy(row + 1)
        return Point(column + CellWidth.of(text), row)
    }

    /** Advances to the next row, leaving the current one blank if nothing was written to it. */
    public fun newLine() {
        row += 1
        occupy(row)
    }

    internal fun build(): ContentLayout = builder.build(occupiedWidth, occupiedHeight)

    private fun occupy(height: Int) {
        if (height > occupiedHeight) occupiedHeight = height
    }
}
