// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Buffer

internal class ViewportTest {

    private val viewportWidth = 26
    private val viewportHeight = 8

    private fun content(lines: Int, revealRow: Int? = null): MeasurableWidget = MeasurableWidget {
        repeat(lines) { row ->
            it.writeLine("line$row")
            if (row == revealRow) it.markRevealAt(row)
        }
    }

    /** The settled frame a viewport publishes to its own state — the caller's only read path. */
    private fun ViewportState.frame(): ScrollState = checkNotNull(settled) { "no drawable frame" }

    @Test
    fun `prepared content renders flush at viewport origin`() {
        val buffer = Buffer(viewportWidth, viewportHeight)
        Viewport(content(5)).render(Canvas.of(buffer), ViewportState())

        assertEquals("line0", buffer.line(0, width = 10))
        assertEquals("line1", buffer.line(1, width = 10))
    }

    @Test
    fun `explicit offset shifts visible window`() {
        val state = ViewportState(ScrollOffset(y = 3))
        val buffer = Buffer(viewportWidth, viewportHeight)
        val viewport = Viewport(content(20))
        viewport.render(Canvas.of(buffer), state)

        assertEquals("line3", buffer.line(0, width = 10))
        assertEquals(12, state.frame().maxOffset.y)
    }

    @Test
    fun `prepared layout reaches rows beyond the old measurement ceiling`() {
        val state = ViewportState(ScrollOffset(y = 999))
        val viewport = Viewport(content(600))
        viewport.render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertEquals(592, state.frame().maxOffset.y)
        assertEquals(592, state.frame().offset.y)
    }

    @Test
    fun `fixed content preserves horizontal and vertical dimensions`() {
        val state = ViewportState()
        Viewport(EmptyWidget.fixedSize(50, 40)).render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertEquals(24, state.frame().maxOffset.x)
        assertEquals(32, state.frame().maxOffset.y)
    }

    @Test
    fun `first render follows a reveal target below the window`() {
        val state = ViewportState()
        Viewport(content(20, revealRow = 15)).render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertTrue(15 in state.frame().offset.y until state.frame().offset.y + viewportHeight)
    }

    @Test
    fun `manual scroll survives an unchanged reveal target`() {
        val state = ViewportState()
        val viewport = Viewport(content(40, revealRow = 15))
        viewport.render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        state.scrollBy(0, -5)
        viewport.render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertEquals(3, state.frame().offset.y)
    }

    @Test
    fun `moved reveal target follows from the current offset`() {
        var revealRow = 19
        val widget = object : LayoutWidget {
            override fun layout(availableWidth: Int): ContentLayout = content(40, revealRow).prepare(availableWidth)
        }
        val state = ViewportState()
        val viewport = Viewport(widget)
        viewport.render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        state.scrollBy(0, -5)
        revealRow = 20
        viewport.render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertTrue(20 in state.frame().offset.y until state.frame().offset.y + viewportHeight)
    }

    @Test
    fun `recenter centers a reveal target`() {
        val state = ViewportState()
        val viewport = Viewport(content(40, revealRow = 20))
        viewport.render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        state.requestRecenter()
        viewport.render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertEquals((20 - (viewportHeight - 1) / 2).coerceIn(0, state.frame().maxOffset.y), state.frame().offset.y)
    }

    @Test
    fun `zero-sized destination preserves the previous settled observation`() {
        val state = ViewportState()
        val viewport = Viewport(content(20))
        viewport.render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        val settled = state.settled
        viewport.render(Canvas.offscreen(30, 0), state)

        assertEquals(settled, state.settled)
    }
}
