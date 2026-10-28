// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Constraint.Fill
import io.archinaut.tenter.screen.Constraint.Length
import io.archinaut.tenter.screen.Constraint.Percentage
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size

internal class RowTest {

    /** Writes [rows] lines of [char] repeated across the available width. */
    private fun filled(char: String, rows: Int = 1): MeasurableWidget = MeasurableWidget { writer ->
        repeat(rows) { writer.writeLine(char.repeat(writer.width)) }
    }

    @Test
    fun `resolves child widths from constraints with spacing between them`() {
        val row = Row(listOf(Length(3) to filled("A"), Fill(1) to filled("B"), Percentage(20) to filled("C")), spacing = 1)

        val buffer = render(row, 12, 1)

        assertEquals("AAA BBBBB CC", buffer.line(0))
    }

    @Test
    fun `never wraps and is as tall as its tallest child`() {
        val row = Row(listOf(Length(8) to filled("A", rows = 1), Length(8) to filled("B", rows = 3)))

        assertEquals(Size(10, 3), row.measure(10))
        assertEquals("AAAAAAAABB", render(row, 10, 3).line(0))
    }

    @Test
    fun `a block around a child draws its border down the full row height`() {
        val divided = filled("D").block(Block(borders = Borders.LEFT, padding = Padding(left = 1)))
        val row = Row(listOf(Length(2) to filled("L", rows = 3), Fill(1) to divided), spacing = 1)

        val buffer = render(row, 7, 3)

        assertEquals(listOf("LL │ DD", "LL │", "LL │"), (0 until 3).map(buffer::line))
    }

    @Test
    fun `a child wider than its width is clipped to it`() {
        val wide = object : Widget {
            override fun render(canvas: Canvas) {
                canvas.setString(0, 0, "XXXXXX")
            }
        }.fixedSize(6, 1)
        val row = Row(listOf(Length(2) to wide, Fill(1) to filled("B")))

        assertEquals("XXBBB", render(row, 5, 1).line(0))
    }

    @Test
    fun `a child's reveal request is offset to its segment`() {
        val revealing = MeasurableWidget { writer ->
            writer.newLine()
            writer.markReveal()
            writer.writeLine("R")
        }
        val row = Row(listOf(Length(3) to filled("A"), Fill(1) to revealing))
        val canvas = Canvas.offscreen(8, 2)

        row.render(canvas)

        assertEquals(Rect(3, 1, 5, 1), canvas.revealRect())
    }

    @Test
    fun `rejects negative spacing`() {
        assertThrows<IllegalArgumentException> { Row(emptyList(), spacing = -1) }
    }
}
