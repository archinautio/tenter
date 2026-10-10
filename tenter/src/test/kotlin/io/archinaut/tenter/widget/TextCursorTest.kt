// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.screen.styled
import io.archinaut.tenter.screen.Rect

internal class TextCursorTest {

    @Test
    fun `immediate and prepared child insertion retain the last nonempty visible reveal`() {
        val cases = listOf(
            fixedContent(4, 1, EmptyWidget) to Rect(0, 0, 4, 1),
            fixedContent(0, 0, EmptyWidget) to Rect(0, 0, 4, 1),
            contentWidget { it.writeLine("child"); it.markRevealAt(0) } to Rect(0, 1, 4, 1),
            fixedContent(4, 1, object : Widget {
                override fun render(canvas: Canvas) { canvas.markReveal(Rect(-2, 0, 3, 1)) }
            }) to Rect(0, 1, 1, 1),
            fixedContent(4, 1, object : Widget {
                override fun render(canvas: Canvas) { canvas.markReveal(Rect(8, 0, 1, 1)) }
            }) to Rect(0, 0, 4, 1),
        )
        for ((child, expected) in cases) {
            val instructions: (TextCursor) -> Unit = { cursor ->
                cursor.writeLine("parent")
                cursor.markRevealAt(0)
                cursor.draw(child)
            }
            val immediate = Canvas.offscreen(4, 3)
            val prepared = Canvas.offscreen(4, 3)

            instructions(TextCursor(immediate))
            contentWidget(instructions).render(prepared)

            assertEquals(expected, immediate.revealRect())
            assertEquals(expected, prepared.revealRect())
        }
    }

    @Test
    fun `a child beyond the allocated canvas does not clear its existing reveal`() {
        val canvas = Canvas.offscreen(4, 1)
        val cursor = TextCursor(canvas)
        cursor.writeLine("parent")
        cursor.markRevealAt(0)

        cursor.draw(contentWidget { it.writeLine("child"); it.markRevealAt(0) })

        assertEquals(Rect(0, 0, 4, 1), canvas.revealRect())
    }

    @Test
    fun `writeLine paints each span in its own style at the right column`() {
        val canvas = Canvas.offscreen(20, 3)
        val cursor = TextCursor(canvas)

        cursor.writeLine(
            styled {
                append("foo", ChromeRole.DANGER)
                append("bar", ChromeRole.SUCCESS)
            },
        )

        assertEquals(Style(ChromeRole.DANGER), canvas.get(0, 0).style)
        assertEquals(Style(ChromeRole.DANGER), canvas.get(2, 0).style)
        assertEquals(Style(ChromeRole.SUCCESS), canvas.get(3, 0).style)
        assertEquals(Style(ChromeRole.SUCCESS), canvas.get(5, 0).style)
        assertEquals("f", canvas.get(0, 0).symbol)
        assertEquals("b", canvas.get(3, 0).symbol)
    }

    @Test
    fun `writeLine advances a wide codepoint's column by two cells`() {
        val canvas = Canvas.offscreen(20, 3)
        val cursor = TextCursor(canvas)

        cursor.writeLine(
            styled {
                append("中", ChromeRole.DANGER)
                append("A", ChromeRole.SUCCESS)
            },
        )

        assertEquals("中", canvas.get(0, 0).symbol)
        assertEquals("", canvas.get(1, 0).symbol, "filler reserves the wide glyph's second cell")
        assertEquals("A", canvas.get(2, 0).symbol)
        assertEquals(Style(ChromeRole.SUCCESS), canvas.get(2, 0).style)
    }

    @Test
    fun `writeLine ellipsizes at the canvas width`() {
        val canvas = Canvas.offscreen(5, 1)
        val cursor = TextCursor(canvas)

        cursor.writeLine(styled { append("hello world", ChromeRole.DANGER) })

        assertEquals("hell…", (0 until 5).joinToString("") { canvas.get(it, 0).symbol })
    }

    @Test
    fun `writeLine advances row by one and returns the row written`() {
        val canvas = Canvas.offscreen(10, 3)
        val cursor = TextCursor(canvas)

        val written = cursor.writeLine(styled { append("a") })

        assertEquals(0, written)
        assertEquals(1, cursor.row)
    }

    @Test
    fun `fixed width write clips before aligning`() {
        val canvas = Canvas.offscreen(12, 1)
        val cursor = TextCursor(canvas)

        cursor.write(column = 2, width = 4, text = "abcdef")

        assertEquals("  abc…      ", (0 until 12).joinToString("") { canvas.get(it, 0).symbol })
    }

    @Test
    fun `fixed width write leaves zero width and adjacent fields harmless`() {
        val canvas = Canvas.offscreen(12, 1)
        val cursor = TextCursor(canvas)

        cursor.write(column = 2, width = 0, text = "ignored")
        cursor.write(column = 6, width = 4, text = "right", align = TextCursor.Align.RIGHT)

        assertEquals("      rig…  ", (0 until 12).joinToString("") { canvas.get(it, 0).symbol })
    }
}
