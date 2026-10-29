// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Constraint.Length

internal class FlowTest {

    /** A [width]x[height] block of [rows] rows, each filled with [char] repeated. */
    private fun block(width: Int, height: Int, char: String): MeasurableWidget = object : Widget {
        override fun render(canvas: Canvas) {
            for (row in 0 until height) canvas.setString(0, row, char.repeat(width))
        }
    }.fixedSize(width, height)

    private fun render(columns: MeasurableWidget, width: Int, height: Int = 10): Buffer {
        val buffer = Buffer(width, height)
        columns.render(Canvas.of(buffer))
        return buffer
    }

    @Test
    fun `places two children side by side, spacing-separated`() {
        val columns = Flow(
            listOf(Length(5) to block(5, 2, "A"), Length(4) to block(4, 2, "B")),
            spacing = 2,
        )

        val buffer = render(columns, width = 40)

        assertEquals("AAAAA  BBBB", buffer.line(0, width = 11))
    }

    @Test
    fun `wraps a child that would not fit to a new band`() {
        val columns = Flow(
            listOf(Length(6) to block(6, 2, "A"), Length(6) to block(6, 3, "B")),
            spacing = 2,
        )

        // Only room for one 6-wide child plus spacing before the edge.
        val buffer = render(columns, width = 10)

        assertEquals("AAAAAA", buffer.line(0, width = 6))
        assertEquals("", buffer.line(0, x = 6, width = 4))
        // New band starts one row below the tallest child in the previous band (height 2).
        assertEquals("BBBBBB", buffer.line(3, width = 6))
    }

    @Test
    fun `bands stack below the tallest child in the previous band`() {
        val columns = Flow(
            listOf(
                Length(4) to block(4, 5, "A"),
                Length(4) to block(4, 2, "B"),
                Length(20) to block(20, 1, "C"),
            ),
            spacing = 1,
        )

        val buffer = render(columns, width = 9)

        // A and B share the first band (4 + 1 + 4 = 9 fits); tallest is A at 5 rows.
        assertEquals("AAAA", buffer.line(0, width = 4))
        assertEquals("BBBB", buffer.line(0, x = 5, width = 4))
        // C doesn't fit next to B (needs 20), so it starts a new band at row 5 + 1 = 6.
        // C keeps its declared 20 cells; only the 9-wide canvas clips it.
        assertEquals("CCCCCCCCC", buffer.line(6, width = 9))
    }

    @Test
    fun `wrapped bands keep one blank row even with no column spacing`() {
        val layout = Flow(
            listOf(
                Length(4) to block(4, 2, "A"),
                Length(4) to block(4, 1, "B"),
            ),
            spacing = 0,
        ).prepare(availableWidth = 4)

        assertEquals(4, layout.height)
    }

    @Test
    fun `a child wider than the available width keeps its declared width`() {
        val columns = Flow(listOf(Length(20) to block(20, 1, "A")))

        assertEquals(20, columns.measure(8).width)
        assertEquals("AAAAAAAA", render(columns, width = 8).line(0, width = 8))
    }

    @Test
    fun `a single child measures to its own content height`() {
        val columns = Flow(listOf(Length(4) to block(4, 3, "A")))

        val buffer = render(columns, width = 10, height = 10)

        assertEquals("AAAA", buffer.line(2, width = 4))
        assertEquals("", buffer.line(3, width = 4))
    }
}
