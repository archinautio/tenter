// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.terminal.CompletedFrame

internal class StackTest {

    /** A [width]x[height] block of rows, each filled with [char] repeated. */
    private fun block(width: Int, height: Int, char: String): MeasurableWidget = object : Widget {
        override fun render(canvas: Canvas) {
            for (row in 0 until height) canvas.setString(0, row, char.repeat(width))
        }
    }.fixedSize(width, height)

    private fun render(stack: MeasurableWidget, width: Int, height: Int = 20): CompletedFrame {
        val buffer = Buffer(width, height)
        stack.render(Canvas.of(buffer))
        return CompletedFrame(buffer)
    }

    @Test
    fun `stacks two children top to bottom, spacing-separated`() {
        val stack = Stack(listOf(block(4, 2, "A"), block(4, 2, "B")), spacing = 1)

        val buffer = render(stack, width = 4)

        assertEquals("AAAA", buffer.region(Rect(0, 0, 4, 1)).line(0))
        assertEquals("AAAA", buffer.region(Rect(0, 1, 4, 1)).line(0))
        assertEquals("", buffer.region(Rect(0, 2, 4, 1)).line(0))
        // Second child starts one row below the first child's measured height (2 + spacing 1 = 3).
        assertEquals("BBBB", buffer.region(Rect(0, 3, 4, 1)).line(0))
        assertEquals("BBBB", buffer.region(Rect(0, 4, 4, 1)).line(0))
    }

    @Test
    fun `each child is placed by its own measured height, not a fixed slot`() {
        val stack = Stack(listOf(block(4, 3, "A"), block(4, 1, "B")), spacing = 0)

        val buffer = render(stack, width = 4)

        assertEquals("AAAA", buffer.region(Rect(0, 2, 4, 1)).line(0))
        assertEquals("BBBB", buffer.region(Rect(0, 3, 4, 1)).line(0))
    }

    @Test
    fun `a child past the bottom of the canvas is skipped`() {
        val stack = Stack(listOf(block(4, 5, "A"), block(4, 5, "B"), block(4, 5, "C")), spacing = 0)

        val buffer = render(stack, width = 4, height = 10)

        assertEquals("AAAA", buffer.region(Rect(0, 0, 4, 1)).line(0))
        assertEquals("BBBB", buffer.region(Rect(0, 5, 4, 1)).line(0))
        // C would start at row 10, at/past the 10-tall canvas — skipped, not drawn out of bounds.
        for (row in 0 until 10) {
            org.junit.jupiter.api.Assertions.assertTrue(
                buffer.region(Rect(0, row, 4, 1)).line(0) != "CCCC",
                "row $row unexpectedly shows C",
            )
        }
    }

    @Test
    fun `a child taller than what remains is clipped, not stretched`() {
        val stack = Stack(listOf(block(4, 8, "A"), block(4, 8, "B")), spacing = 0)

        val buffer = render(stack, width = 4, height = 10)

        assertEquals("AAAA", buffer.region(Rect(0, 7, 4, 1)).line(0))
        // B starts at row 8, but the canvas is only 10 tall — only rows 8-9 of B fit.
        assertEquals("BBBB", buffer.region(Rect(0, 8, 4, 1)).line(0))
        assertEquals("BBBB", buffer.region(Rect(0, 9, 4, 1)).line(0))
    }

    @Test
    fun `a single child measures to its own content height`() {
        val stack = Stack(listOf(block(4, 3, "A")))

        val buffer = render(stack, width = 10, height = 10)

        assertEquals("AAAA", buffer.region(Rect(0, 2, 4, 1)).line(0))
        assertEquals("", buffer.region(Rect(0, 3, 4, 1)).line(0))
    }
}
