// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CompletedFrameTest {

    @Test
    fun `a line is right-trimmed and shows a wide character once`() {
        assertEquals("日本 x", CompletedFrame.of("日本 x  ").line(0))
    }

    @Test
    fun `text joins trimmed rows without the blank ones at the bottom`() {
        assertEquals("ab\n c", CompletedFrame.of("ab ", " c", "", "  ").text())
    }

    @Test
    fun `find answers in cell coordinates, past wide characters`() {
        val frame = CompletedFrame.of("日本 x", "  yz")

        assertEquals(Point(5, 0), frame.find("x"))
        assertEquals(Point(3, 1), frame.find("z"))
        assertNull(frame.find("w"))
    }

    @Test
    fun `a region reads in its own coordinates`() {
        val region = CompletedFrame.of("abc", "def").region(Rect(1, 1, 2, 1))

        assertEquals("ef", region.line(0))
        assertEquals("e", region.get(0, 0).symbol)
        assertEquals(Point(1, 0), region.find("f"))
        assertThrows<IndexOutOfBoundsException> { region.get(2, 0) }
    }

    @Test
    fun `a region must lie inside the frame`() {
        assertThrows<IndexOutOfBoundsException> { CompletedFrame.of("abc").region(Rect(2, 0, 2, 1)) }
    }

    @Test
    fun `equal frames ignore where they sit on screen`() {
        val region = CompletedFrame.of("xab").region(Rect(1, 0, 2, 1))

        assertEquals(CompletedFrame.of("ab"), region)
        assertEquals(CompletedFrame.of("ab").hashCode(), region.hashCode())
    }

    @Test
    fun `equal frames compare styles as the terminal renders them`() {
        val unset = CompletedFrame.of("a")

        assertEquals(unset, CompletedFrame.of(Line("a", Style(fg = StandardRole.DEFAULT, bold = false))))
        assertEquals(unset.hashCode(), CompletedFrame.of(Line("a", Style(fg = StandardRole.DEFAULT))).hashCode())
        assertNotEquals(unset, CompletedFrame.of(Line("a", Style(fg = StandardRole.ACCENT))))
    }

    @Test
    fun `frames of different sizes differ`() {
        assertNotEquals(CompletedFrame.of("a"), CompletedFrame.of("a "))
    }

    @Test
    fun `of pads shorter lines to the widest`() {
        val frame = CompletedFrame.of("abc", "a")

        assertEquals(3, frame.width)
        assertEquals(2, frame.height)
        assertEquals(" ", frame.get(2, 1).symbol)
    }

    @Test
    fun `toString shows quoted rows and each change of style`() {
        val frame = CompletedFrame.of(Line(Span("ab", Style(fg = StandardRole.ACCENT, bold = true)), Span(" ")))

        assertEquals(
            """
            |CompletedFrame 3x1
            |  "ab "
            |styles:
            |  (0,0) fg=ACCENT bg=DEFAULT bold
            |  (2,0) fg=DEFAULT bg=DEFAULT
            |
            """.trimMargin(),
            frame.toString(),
        )
    }
}
