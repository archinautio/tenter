// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Rect
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
            val line = Paragraph("line$row")
            it.draw(if (row == revealRow) line.revealed() else line)
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
        val state = ViewportState().apply { scrollBy(0, 3) }
        val buffer = Buffer(viewportWidth, viewportHeight)
        val viewport = Viewport(content(20))
        viewport.render(Canvas.of(buffer), state)

        assertEquals("line3", buffer.line(0, width = 10))
        assertEquals(12, state.frame().maxOffset.y)
    }

    @Test
    fun `prepared layout reaches rows beyond the old measurement ceiling`() {
        val state = ViewportState().apply { scrollBy(0, 999) }
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

    @Test
    fun `a block on the content scrolls away with it`() {
        val state = ViewportState().apply { scrollBy(0, 1) }

        val buffer = render(Viewport(content(5).block(Block.bordered())), 10, 3, state)

        assertEquals("│line0", buffer.line(0, width = 6))
    }

    @Test
    fun `content following its end keeps the last row visible as it grows`() {
        val state = ViewportState()

        Viewport(content(20).followingEnd()).render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        assertEquals(12, state.frame().offset.y)
        Viewport(content(30).followingEnd()).render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertEquals(22, state.frame().offset.y)
    }

    @Test
    fun `scrolling up stops following the end and scrolling back resumes it`() {
        val state = ViewportState()
        Viewport(content(20).followingEnd()).render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        state.scrollBy(0, -5)
        Viewport(content(30).followingEnd()).render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        assertEquals(7, state.frame().offset.y)

        state.scrollBy(0, 100)
        Viewport(content(30).followingEnd()).render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        Viewport(content(40).followingEnd()).render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        assertEquals(32, state.frame().offset.y)
    }

    @Test
    fun `content not following its end keeps the offset as it grows`() {
        val state = ViewportState()

        Viewport(content(20)).render(Canvas.offscreen(viewportWidth, viewportHeight), state)
        Viewport(content(30)).render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertEquals(0, state.frame().offset.y)
    }

    @Test
    fun `a plain widget reveals the area it is given`() {
        val state = ViewportState()

        Viewport(EmptyWidget.fixedSize(10, 40).revealed(Rect(0, 30, 2, 2))).render(Canvas.offscreen(viewportWidth, viewportHeight), state)

        assertEquals(24, state.frame().offset.y)
        assertEquals(Rect(0, 30, 2, 2), state.frame().revealed)
    }

    @Test
    fun `a plain widget is painted once per frame, straight into the viewport`() {
        val painted = mutableListOf<Int>()
        val tall = object : Widget {
            override fun render(canvas: Canvas) {
                painted += canvas.height
            }
        }
        val state = ViewportState().apply { scrollBy(0, 5) }

        val buffer = render(Viewport(tall.fixedSize(4, 40)), 4, 3, state)

        assertEquals(listOf(40), painted)
        assertEquals(3, buffer.height)
    }
}
