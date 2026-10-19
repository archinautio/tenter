// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.palette.ChromeRole

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CanvasTest {

    @Test
    fun `get and set within bounds`() {
        val canvas = Canvas.offscreen(5, 5)
        val cell = Cell("A", Style(ChromeRole.DANGER, ChromeRole.INFO))

        canvas.set(2, 3, cell)

        assertEquals(cell, canvas.get(2, 3))
    }

    @Test
    fun `set out of bounds is ignored`() {
        val canvas = Canvas.offscreen(3, 3)

        canvas.set(-1, 0, Cell("X"))
        canvas.set(0, -1, Cell("X"))
        canvas.set(3, 0, Cell("X"))
        canvas.set(0, 3, Cell("X"))

        assertEquals(Cell(), canvas.get(0, 0))
    }

    @Test
    fun `get out of bounds throws`() {
        val canvas = Canvas.offscreen(3, 3)

        assertThrows<IndexOutOfBoundsException> { canvas.get(-1, 0) }
        assertThrows<IndexOutOfBoundsException> { canvas.get(3, 0) }
        assertThrows<IndexOutOfBoundsException> { canvas.get(0, -1) }
        assertThrows<IndexOutOfBoundsException> { canvas.get(0, 3) }
    }

    @Test
    fun `setString places characters horizontally`() {
        val canvas = Canvas.offscreen(10, 1)

        canvas.setString(2, 0, "Hi", Style(ChromeRole.SUCCESS, ChromeRole.TEXT_MUTED))

        assertEquals(Cell("H", Style(ChromeRole.SUCCESS, ChromeRole.TEXT_MUTED)), canvas.get(2, 0))
        assertEquals(Cell("i", Style(ChromeRole.SUCCESS, ChromeRole.TEXT_MUTED)), canvas.get(3, 0))
        assertEquals(Cell(), canvas.get(4, 0))
    }

    @Test
    fun `setString truncates at canvas edge, not the underlying buffer edge`() {
        val buffer = Buffer(20, 1)
        val canvas = Canvas.of(buffer).region(Rect(3, 0, 5, 1))

        canvas.setString(0, 0, "Hello world")

        assertEquals("Hello", (0 until 5).joinToString("") { canvas.get(it, 0).symbol })
        // the sixth character must not have leaked past the canvas into buffer column 8
        assertEquals(Cell(), buffer.get(8, 0))
    }

    @Test
    fun `setString at a negative x drops leading cells and lands the tail at column 0`() {
        val canvas = Canvas.offscreen(5, 1)

        canvas.setString(-2, 0, "ABCDE")

        assertEquals("C", canvas.get(0, 0).symbol)
        assertEquals("D", canvas.get(1, 0).symbol)
        assertEquals("E", canvas.get(2, 0).symbol)
    }

    @Test
    fun `setString uses default colors when not specified`() {
        val canvas = Canvas.offscreen(5, 1)

        canvas.setString(0, 0, "AB")

        assertEquals(Cell("A", Style(ChromeRole.DEFAULT, ChromeRole.DEFAULT)), canvas.get(0, 0))
    }

    @Test
    fun `setString puts a non-BMP nerd-font icon in a single cell`() {
        val canvas = Canvas.offscreen(10, 1)
        val die = String(Character.toChars(0xF01CA)) // nf-md-dice_1, 2 UTF-16 chars

        canvas.setString(2, 0, "A" + die + "B")

        assertEquals("A", canvas.get(2, 0).symbol)
        assertEquals(die, canvas.get(3, 0).symbol)
        assertEquals(2, canvas.get(3, 0).symbol.length) // surrogate pair preserved as one cell
        assertEquals("B", canvas.get(4, 0).symbol)
        assertEquals(" ", canvas.get(5, 0).symbol)
    }

    @Test
    fun `setString reserves two cells for an East-Asian wide codepoint`() {
        val canvas = Canvas.offscreen(10, 1)

        canvas.setString(0, 0, "中A")

        assertEquals("中", canvas.get(0, 0).symbol)
        assertEquals("", canvas.get(1, 0).symbol) // filler reserves the second visual cell
        assertEquals("A", canvas.get(2, 0).symbol)
    }

    @Test
    fun `setString preserves zero-width combining marks in their cluster`() {
        val canvas = Canvas.offscreen(5, 1)

        // 'e' + combining acute (U+0301) + 'x'
        canvas.setString(0, 0, "e\u0301x")

        assertEquals("e\u0301", canvas.get(0, 0).symbol)
        assertEquals("x", canvas.get(1, 0).symbol)
        assertEquals(" ", canvas.get(2, 0).symbol)
    }

    @Test
    fun `setString replaces isolated surrogates and skips terminal controls`() {
        val canvas = Canvas.offscreen(5, 1)

        canvas.setString(0, 0, "\uD800\u001BA")

        assertEquals("�", canvas.get(0, 0).symbol)
        assertEquals("A", canvas.get(1, 0).symbol)
        assertEquals(" ", canvas.get(2, 0).symbol)
    }

    @Test
    fun `width and height are accessible`() {
        val canvas = Canvas.offscreen(10, 20)

        assertEquals(10, canvas.width)
        assertEquals(20, canvas.height)
    }

    @Test
    fun `region derives a canvas at a local offset`() {
        val buffer = Buffer(15, 8)
        val canvas = Canvas.of(buffer).region(Rect(2, 1, 10, 5))

        canvas.set(0, 0, Cell("X"))

        assertEquals(Cell("X"), buffer.get(2, 1))
    }

    @Test
    fun `region clamps to the parent rather than escaping it`() {
        val canvas = Canvas.offscreen(10, 10)

        val child = canvas.region(Rect(8, 8, 100, 100))

        assertEquals(2, child.width)
        assertEquals(2, child.height)
    }

    @Test
    fun `region intersects a rectangle that starts outside the parent`() {
        val buffer = Buffer(10, 1)
        val child = Canvas.of(buffer).region(Rect(-2, 0, 5, 1))

        assertEquals(3, child.width)
        child.set(2, 0, Cell("X"))

        assertEquals(Cell("X"), buffer.get(2, 0))
        assertEquals(Cell(), buffer.get(3, 0))
    }

    @Test
    fun `reveal intersection does not retain hidden columns`() {
        val canvas = Canvas.offscreen(10, 1)

        canvas.markReveal(Rect(-2, 0, 5, 1))

        assertEquals(Rect(0, 0, 3, 1), canvas.revealRect())
    }

    @Test
    fun `inset composes across nesting and clamps to zero rather than negative`() {
        val canvas = Canvas.offscreen(10, 10)

        val once = canvas.inner(Padding(2, 2, 2, 2))
        assertEquals(6, once.width)
        assertEquals(6, once.height)

        val twice = once.inner(Padding(2, 2, 2, 2))
        assertEquals(2, twice.width)
        assertEquals(2, twice.height)

        val overInset = twice.inner(Padding.uniform(5))
        assertEquals(0, overInset.width)
        assertEquals(0, overInset.height)
    }

    @Test
    fun `a canvas cannot write past its own edge into a sibling region`() {
        val buffer = Buffer(10, 1)
        val left = Canvas.of(buffer).region(Rect(0, 0, 5, 1))
        val right = Canvas.of(buffer).region(Rect(5, 0, 5, 1))

        left.setString(0, 0, "AAAAAAAAAA") // ten chars into a five-wide canvas

        assertEquals(Cell(), right.get(0, 0))
        assertEquals(Cell(), buffer.get(5, 0))
    }

    @Test
    fun `setFg preserves the existing background`() {
        val canvas = Canvas.offscreen(5, 1)
        canvas.set(0, 0, Cell(style = Style(ChromeRole.DEFAULT, ChromeRole.INFO)))

        canvas.setFg(0, 0, "X", ChromeRole.DANGER)

        assertEquals(Cell("X", Style(ChromeRole.DANGER, ChromeRole.INFO)), canvas.get(0, 0))
    }

    @Test
    fun `setFg outside the canvas is a silent no-op, never throws`() {
        val canvas = Canvas.offscreen(3, 3)

        canvas.setFg(-1, 0, "X", ChromeRole.DANGER)
        canvas.setFg(3, 0, "X", ChromeRole.DANGER)

        assertEquals(Cell(), canvas.get(0, 0))
    }

    @Test
    fun `a wide glyph at the child edge paints a blank instead of a half glyph`() {
        val buffer = Buffer(4, 1)
        val child = Canvas.of(buffer).region(Rect(1, 0, 1, 1))

        child.setString(0, 0, "中", Style(fg = ChromeRole.ACCENT))

        assertEquals(Cell(" ", Style(fg = ChromeRole.ACCENT)), buffer.get(1, 0))
        assertEquals(Cell(), buffer.get(2, 0))
    }

    @Test
    fun `a wide glyph clipped on the left paints only a blank surviving slot`() {
        val canvas = Canvas.offscreen(3, 1)

        canvas.setString(-1, 0, "中", Style(fg = ChromeRole.ACCENT))

        assertEquals(Cell(" ", Style(fg = ChromeRole.ACCENT)), canvas.get(0, 0))
        assertEquals(Cell(), canvas.get(1, 0))
    }

    @Test
    fun `replacing either half of a wide glyph removes the complete glyph`() {
        val canvas = Canvas.offscreen(4, 1)
        canvas.setString(0, 0, "中A")

        canvas.set(1, 0, Cell("X"))

        assertEquals(Cell(), canvas.get(0, 0))
        assertEquals(Cell("X"), canvas.get(1, 0))
        assertEquals(Cell("A"), canvas.get(2, 0))
    }

    @Test
    fun `blit copies cell char fg and bg from source to destination`() {
        val src = Canvas.offscreen(5, 5)
        src.set(1, 2, Cell("X", Style(ChromeRole.DANGER, ChromeRole.INFO)))
        val dest = Canvas.offscreen(10, 10)

        dest.blit(src.region(Rect(1, 2, 1, 1)), 3, 4)

        assertEquals(Cell("X", Style(ChromeRole.DANGER, ChromeRole.INFO)), dest.get(3, 4))
    }

    @Test
    fun `blit clips at destination right and bottom edges`() {
        val src = Canvas.offscreen(5, 5)
        src.set(0, 0, Cell("A", Style(ChromeRole.SUCCESS)))
        src.set(1, 0, Cell("B", Style(ChromeRole.SUCCESS)))
        src.set(2, 0, Cell("C", Style(ChromeRole.SUCCESS)))
        val dest = Canvas.offscreen(4, 4)

        dest.blit(src.region(Rect(0, 0, 3, 1)), 3, 0)

        assertEquals(Cell("A", Style(ChromeRole.SUCCESS)), dest.get(3, 0))
        assertEquals(Cell(), dest.get(2, 0))
    }

    @Test
    fun `blit of a source region larger than its canvas copies only the canvas`() {
        val src = Canvas.offscreen(2, 2)
        src.set(0, 0, Cell("Z", Style(ChromeRole.ACCENT)))
        val dest = Canvas.offscreen(10, 10)

        dest.blit(src.region(Rect(0, 0, 5, 5)))

        assertEquals(Cell("Z", Style(ChromeRole.ACCENT)), dest.get(0, 0))
        assertEquals(Cell(), dest.get(2, 0))
        assertEquals(Cell(), dest.get(0, 2))
    }

    @Test
    fun `blit into a sub-region never writes past that region's own edge`() {
        val buffer = Buffer(10, 1)
        val src = Canvas.offscreen(5, 1)
        src.set(0, 0, Cell("A"))
        src.set(1, 0, Cell("B"))
        src.set(2, 0, Cell("C"))
        val dest = Canvas.of(buffer).region(Rect(0, 0, 2, 1)) // narrower than the 3-cell blit

        dest.blit(src.region(Rect(0, 0, 3, 1)))

        assertEquals(Cell(), buffer.get(2, 0), "must not leak past the 2-wide dest canvas")
    }

    @Test
    fun `blit never copies only half of a wide glyph`() {
        val source = Canvas.offscreen(3, 1)
        source.setString(0, 0, "中")
        val destination = Canvas.offscreen(3, 1)

        destination.blit(source.region(Rect(0, 0, 1, 1)))

        assertEquals(Cell(), destination.get(0, 0))
        assertEquals(Cell(), destination.get(1, 0))
    }

    @Test
    fun `overlapping horizontal and vertical blits use their original source`() {
        val horizontal = Canvas.offscreen(4, 1)
        horizontal.setString(0, 0, "ABCD")
        horizontal.blit(horizontal.region(Rect(0, 0, 3, 1)), 1, 0)

        val vertical = Canvas.offscreen(3, 3)
        vertical.setString(0, 0, "ABC")
        vertical.setString(0, 1, "DEF")
        vertical.blit(vertical.region(Rect(0, 0, 3, 2)), 0, 1)

        assertEquals("AABC", (0 until 4).joinToString("") { horizontal.get(it, 0).symbol })
        assertEquals("ABC", (0 until 3).joinToString("") { vertical.get(it, 1).symbol })
        assertEquals("DEF", (0 until 3).joinToString("") { vertical.get(it, 2).symbol })
    }

    @Test
    fun `two regions over one buffer overlap, so their blit still reads original source`() {
        // Distinct Canvas objects, one backing buffer: the shifted copy would read cells this same
        // blit has already overwritten unless overlap is detected by buffer, not by canvas identity.
        val canvas = Canvas.offscreen(6, 1)
        canvas.setString(0, 0, "ABCDEF")

        canvas.region(Rect(1, 0, 5, 1)).blit(canvas.region(Rect(0, 0, 5, 1)))

        assertEquals("AABCDE", (0 until 6).joinToString("") { canvas.get(it, 0).symbol })
    }

    @Test
    fun `a window keeps a negative origin and writes only where it overlaps its parent`() {
        val buffer = Buffer(6, 2)
        val parent = Canvas.of(buffer).region(Rect(1, 0, 4, 2))
        val window = parent.window(Rect(-2, 1, 10, 1))

        window.setString(0, 0, "abcdefgh")

        assertEquals(Rect(2, 0, 4, 1), window.visibleArea)
        assertEquals(" cdef ", (0 until 6).joinToString("") { buffer.get(it, 1).symbol })
        assertEquals(Cell(), window.get(0, 0))
    }

    @Test
    fun `a wide glyph across a window's left edge paints only a blank surviving slot`() {
        val canvas = Canvas.offscreen(4, 1)
        val window = canvas.window(Rect(-1, 0, 5, 1))

        window.setString(0, 0, "中x", Style(fg = ChromeRole.ACCENT))

        assertEquals(Cell(" ", Style(fg = ChromeRole.ACCENT)), canvas.get(0, 0))
        assertEquals("x", canvas.get(1, 0).symbol)
    }

    @Test
    fun `blit into a window copies only its visible part`() {
        val source = Canvas.offscreen(3, 3)
        (0 until 3).forEach { row -> source.setString(0, row, "$row$row$row") }
        val canvas = Canvas.offscreen(3, 1)

        canvas.window(Rect(0, -1, 3, 3)).blit(source)

        assertEquals("111", (0 until 3).joinToString("") { canvas.get(it, 0).symbol })
    }
}
