// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size

internal class RowWriterTest {

    /** Writes [block] as a measurable widget painted into a fresh [width]x[height] canvas. */
    private fun paint(width: Int, height: Int, block: (RowWriter) -> Unit): Canvas =
        Canvas.offscreen(width, height).also { MeasurableWidget(block).render(it) }

    @Test
    fun `child insertion retains the last nonempty visible reveal`() {
        val cases = listOf(
            EmptyWidget.fixedSize(4, 1) to Rect(0, 0, 4, 1),
            EmptyWidget.fixedSize(0, 0) to Rect(0, 0, 4, 1),
            MeasurableWidget { it.writeLine("child"); it.markReveal(row = 0) } to Rect(0, 1, 4, 1),
            object : Widget {
                override fun render(canvas: Canvas) { canvas.markReveal(Rect(-2, 0, 3, 1)) }
            }.fixedSize(4, 1) to Rect(0, 1, 1, 1),
            object : Widget {
                override fun render(canvas: Canvas) { canvas.markReveal(Rect(8, 0, 1, 1)) }
            }.fixedSize(4, 1) to Rect(0, 0, 4, 1),
        )
        for ((child, expected) in cases) {
            val canvas = paint(4, 3) { writer ->
                writer.writeLine("parent")
                writer.markReveal(row = 0)
                writer.draw(child)
            }

            assertEquals(expected, canvas.revealRect())
        }
    }

    @Test
    fun `a drawn child wider than the writer widens the widget, as a stacked one does`() {
        val wide = EmptyWidget.fixedSize(30, 2)

        val written = MeasurableWidget { it.writeLine("list"); it.draw(wide) }.measure(10)

        assertEquals(Size(30, 3), written)
        assertEquals(Stack(listOf(wide), gutter = 0).measure(10).width, written.width)
    }

    @Test
    fun `text past the writer width does not widen the widget`() {
        val written = MeasurableWidget { it.write(8, "overflowing") }.measure(10)

        assertEquals(Size(10, 1), written)
    }

    @Test
    fun `writeLine paints each span in its own style at the right column`() {
        val canvas = paint(20, 3) { writer ->
            writer.writeLine(Line(Span("foo", Style(StandardRole.DANGER)), Span("bar", Style(StandardRole.SUCCESS))))
        }

        assertEquals(Style(StandardRole.DANGER), canvas.get(0, 0).style)
        assertEquals(Style(StandardRole.DANGER), canvas.get(2, 0).style)
        assertEquals(Style(StandardRole.SUCCESS), canvas.get(3, 0).style)
        assertEquals(Style(StandardRole.SUCCESS), canvas.get(5, 0).style)
        assertEquals("f", canvas.get(0, 0).symbol)
        assertEquals("b", canvas.get(3, 0).symbol)
    }

    @Test
    fun `writeLine advances a wide codepoint's column by two cells`() {
        val canvas = paint(20, 3) { writer ->
            writer.writeLine(Line(Span("中", Style(StandardRole.DANGER)), Span("A", Style(StandardRole.SUCCESS))))
        }

        assertEquals("中", canvas.get(0, 0).symbol)
        assertEquals("", canvas.get(1, 0).symbol, "filler reserves the wide glyph's second cell")
        assertEquals("A", canvas.get(2, 0).symbol)
        assertEquals(Style(StandardRole.SUCCESS), canvas.get(2, 0).style)
    }

    @Test
    fun `writeLine ellipsizes at the available width`() {
        val canvas = paint(5, 1) { writer -> writer.writeLine("hello world", Style(StandardRole.DANGER)) }

        assertEquals("hell…", (0 until 5).joinToString("") { canvas.get(it, 0).symbol })
    }

    @Test
    fun `writeLine advances row by one and returns where the line ended`() {
        var ended: Point? = null
        var rowAfter = -1
        paint(10, 3) { writer ->
            writer.newLine()
            ended = writer.writeLine("中a")
            rowAfter = writer.row
        }

        assertEquals(Point(3, 1), ended)
        assertEquals(2, rowAfter)
    }

    @Test
    fun `writeLine reports the ellipsized end`() {
        var ended: Point? = null
        paint(5, 1) { writer -> ended = writer.writeLine("hello world") }

        assertEquals(Point(5, 0), ended)
    }

    @Test
    fun `write returns the column after the text so pieces can follow each other`() {
        val canvas = paint(8, 1) { writer ->
            val label = writer.write(0, "HP:")
            writer.write(label.x + 1, "中!")
        }

        assertEquals("HP: 中!", (0 until 8).joinToString("") { canvas.get(it, 0).symbol }.trimEnd())
    }

    @Test
    fun `write overlays an earlier row without advancing`() {
        var rowAfter = -1
        val canvas = paint(6, 2) { writer ->
            writer.writeLine("ab")
            writer.writeLine("cd")
            writer.write(column = 3, text = "X", row = 0)
            rowAfter = writer.row
        }

        assertEquals("ab X  ", (0 until 6).joinToString("") { canvas.get(it, 0).symbol })
        assertEquals(2, rowAfter)
    }

    @Test
    fun `markReveal defaults to the current row and accepts an explicit one`() {
        val current = paint(4, 3) { writer ->
            writer.newLine()
            writer.markReveal(height = 2)
            writer.newLine()
            writer.newLine()
        }
        val explicit = paint(4, 3) { writer ->
            writer.newLine()
            writer.newLine()
            writer.markReveal(row = 0)
        }

        assertEquals(Rect(0, 1, 4, 2), current.revealRect())
        assertEquals(Rect(0, 0, 4, 1), explicit.revealRect())
    }
}
