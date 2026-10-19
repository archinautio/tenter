// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.screen.Rect

internal class TextCursorTest {

    /** Writes [block] as a measurable widget painted into a fresh [width]x[height] canvas. */
    private fun paint(width: Int, height: Int, block: (TextCursor) -> Unit): Canvas =
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
            val canvas = paint(4, 3) { cursor ->
                cursor.writeLine("parent")
                cursor.markReveal(row = 0)
                cursor.draw(child)
            }

            assertEquals(expected, canvas.revealRect())
        }
    }

    @Test
    fun `writeLine paints each span in its own style at the right column`() {
        val canvas = paint(20, 3) { cursor ->
            cursor.writeLine(Line(Span("foo", Style(ChromeRole.DANGER)), Span("bar", Style(ChromeRole.SUCCESS))))
        }

        assertEquals(Style(ChromeRole.DANGER), canvas.get(0, 0).style)
        assertEquals(Style(ChromeRole.DANGER), canvas.get(2, 0).style)
        assertEquals(Style(ChromeRole.SUCCESS), canvas.get(3, 0).style)
        assertEquals(Style(ChromeRole.SUCCESS), canvas.get(5, 0).style)
        assertEquals("f", canvas.get(0, 0).symbol)
        assertEquals("b", canvas.get(3, 0).symbol)
    }

    @Test
    fun `writeLine advances a wide codepoint's column by two cells`() {
        val canvas = paint(20, 3) { cursor ->
            cursor.writeLine(Line(Span("中", Style(ChromeRole.DANGER)), Span("A", Style(ChromeRole.SUCCESS))))
        }

        assertEquals("中", canvas.get(0, 0).symbol)
        assertEquals("", canvas.get(1, 0).symbol, "filler reserves the wide glyph's second cell")
        assertEquals("A", canvas.get(2, 0).symbol)
        assertEquals(Style(ChromeRole.SUCCESS), canvas.get(2, 0).style)
    }

    @Test
    fun `writeLine ellipsizes at the available width`() {
        val canvas = paint(5, 1) { cursor -> cursor.writeLine("hello world", Style(ChromeRole.DANGER)) }

        assertEquals("hell…", (0 until 5).joinToString("") { canvas.get(it, 0).symbol })
    }

    @Test
    fun `writeLine advances row by one and returns the row written`() {
        var written = -1
        var rowAfter = -1
        paint(10, 3) { cursor ->
            written = cursor.writeLine("a")
            rowAfter = cursor.row
        }

        assertEquals(0, written)
        assertEquals(1, rowAfter)
    }

    @Test
    fun `write overlays an earlier row without advancing`() {
        var rowAfter = -1
        val canvas = paint(6, 2) { cursor ->
            cursor.writeLine("ab")
            cursor.writeLine("cd")
            cursor.write(column = 3, text = "X", row = 0)
            rowAfter = cursor.row
        }

        assertEquals("ab X  ", (0 until 6).joinToString("") { canvas.get(it, 0).symbol })
        assertEquals(2, rowAfter)
    }

    @Test
    fun `markReveal defaults to the current row and accepts an explicit one`() {
        val current = paint(4, 3) { cursor ->
            cursor.newLine()
            cursor.markReveal(height = 2)
            cursor.newLine()
            cursor.newLine()
        }
        val explicit = paint(4, 3) { cursor ->
            cursor.newLine()
            cursor.newLine()
            cursor.markReveal(row = 0)
        }

        assertEquals(Rect(0, 1, 4, 2), current.revealRect())
        assertEquals(Rect(0, 0, 4, 1), explicit.revealRect())
    }
}
