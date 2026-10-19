// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.widget.Columns
import io.archinaut.tenter.widget.Column
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Stack
import io.archinaut.tenter.widget.TextCursor
import io.archinaut.tenter.widget.CheckState
import io.archinaut.tenter.widget.CheckboxGlyphs
import io.archinaut.tenter.widget.SelectableRow

/** The generated catalog content, deliberately composed from stacked and column widgets. */
internal class ExampleListView(
    private val rowCount: Int,
    private val state: ExampleState,
) : MeasurableWidget {
    override fun write(cursor: TextCursor) {
        val markerColumn = MeasurableWidget { column ->
            repeat(rowCount) { index ->
                column.writeLine(if (index == state.selectedRow) "▶" else " ", Style(ChromeRole.ACCENT))
            }
        }
        val rowColumnWidth = (cursor.width - COLUMN_GUTTER - MARKER_WIDTH).coerceAtLeast(0)
        val rowColumn = MeasurableWidget { column ->
            repeat(rowCount) { index ->
                val label = when (index % 3) {
                    0 -> "row $index — ordinary text"
                    1 -> "row $index — 中 rendered as a wide glyph"
                    else -> "row $index — decomposed e\u0301 accent"
                }
                // --8<-- [start:rows]
                column.draw(
                    SelectableRow(
                        label = label,
                        checkState = if (index in state.checkedRows) CheckState.CHECKED else CheckState.UNCHECKED,
                        selected = index == state.selectedRow,
                        glyphs = CheckboxGlyphs.ASCII,
                    ),
                )
                // --8<-- [end:rows]
            }
        }
        // --8<-- [start:columns]
        val catalog = Columns(
            children = listOf(
                Column(MARKER_WIDTH, markerColumn),
                Column(rowColumnWidth, rowColumn),
            ),
            gutter = COLUMN_GUTTER,
        )
        // --8<-- [end:columns]
        val footer = MeasurableWidget { text ->
            text.newLine()
            text.writeLine("600 rows • CJK 中 • decomposed e\u0301 • click or use the keyboard")
        }
        cursor.draw(Stack(listOf(catalog, footer)))
    }

    private companion object {
        private const val MARKER_WIDTH: Int = 2
        private const val COLUMN_GUTTER: Int = 1
    }
}
