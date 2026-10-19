// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Buffer

internal class SelectableRowTest {

    @Test
    fun `draws a highlighted row with its cursor and checkbox`() {
        val (buffer, rows) = draw(SelectableRow(label = "host", checkState = CheckState.UNCHECKED, selected = true))

        assertEquals("▶", buffer.get(0, 0).symbol)
        assertEquals("host", rowText(buffer, 4, 4))
        assertEquals(ChromeRole.ACCENT, buffer.get(4, 0).style.fg)
        assertEquals(ChromeRole.ACCENT, buffer.get(2, 0).style.fg)
        assertEquals(1, rows)
    }

    @Test
    fun `draws value rows and preserves custom disabled styling`() {
        val (buffer, rows) = draw(
            SelectableRow(
                label = "AC/20",
                checkState = CheckState.INDETERMINATE,
                selected = false,
                right = "58%",
                subLines = listOf("+2 range"),
                textColor = ChromeRole.DISABLED,
                checkboxColor = ChromeRole.DISABLED,
            ),
        )

        assertEquals(" ", buffer.get(0, 0).symbol)
        assertEquals("AC/20", rowText(buffer, 4, 5))
        assertEquals(ChromeRole.DISABLED, buffer.get(4, 0).style.fg)
        assertEquals(ChromeRole.DISABLED, buffer.get(2, 0).style.fg)
        assertEquals("+2 range", rowText(buffer, 4, 8, row = 1))
        assertEquals(2, rows)
    }

    @Test
    fun `uses the requested checkbox glyph set`() {
        val (buffer, _) = draw(
            SelectableRow(label = "ascii", checkState = CheckState.CHECKED, selected = false, glyphs = CheckboxGlyphs.ASCII),
        )

        assertEquals("x", buffer.get(2, 0).symbol)
    }

    @Test
    fun `a selected row deep in a list is followed by the enclosing viewport`() {
        val rows = MeasurableWidget { cursor ->
            repeat(50) { index ->
                cursor.draw(SelectableRow(label = "row $index", checkState = CheckState.UNCHECKED, selected = index == 40))
            }
        }
        val state = ViewportState()

        val buffer = render(Viewport(rows), 20, 5, state)

        assertEquals(40, checkNotNull(state.settled).revealed?.y)
        assertEquals("▶ □ row 40", buffer.line(4))
    }

    /** Renders [row] into a fresh buffer and returns it with the row's measured height. */
    private fun draw(row: MeasurableWidget, width: Int = 32, height: Int = 5): Pair<Buffer, Int> =
        render(row, width, height) to row.measure(width).height

    private fun rowText(buffer: Buffer, start: Int, length: Int, row: Int = 0): String =
        (start until start + length).joinToString("") { buffer.get(it, row).symbol }
}
