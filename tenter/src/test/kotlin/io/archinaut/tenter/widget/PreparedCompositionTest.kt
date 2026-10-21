// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Rect


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Buffer

internal class PreparedCompositionTest {

    @Test
    fun `width-only resize follows a fixed reveal and unchanged dimensions retain manual scroll`() {
        val state = ViewportState()
        val viewport = Viewport(object : Widget {
            override fun render(canvas: Canvas) { canvas.markReveal(Rect(15, 0, 1, 1)) }
        }.fixedSize(30, 1))
        viewport.render(Canvas.offscreen(20, 2), state)

        viewport.render(Canvas.offscreen(10, 2), state)
        assertEquals(6, state.settled?.offset?.x)
        state.scrollBy(-100, 0)
        viewport.render(Canvas.offscreen(10, 2), state)
        assertEquals(0, state.settled?.offset?.x)
        viewport.render(Canvas.offscreen(12, 2), state)

        assertEquals(4, state.settled?.offset?.x)
    }

    @Test
    fun `stack uses logical child heights and no trailing gutter`() {
        val stack = Stack(
            listOf(
                MeasurableWidget { it.writeLine("A"); it.newLine() },
                MeasurableWidget { it.writeLine("B") },
            ),
            gutter = 2,
        )

        val layout = stack.prepare(4)

        assertEquals(4, layout.width)
        assertEquals(5, layout.height)
        assertEquals("B", render(layout, 4, 5).line(4, width = 4))
    }

    @Test
    fun `columns wraps by declared widths and logical heights`() {
        val columns = Columns(
            listOf(
                Column(4, MeasurableWidget { it.writeLine("AAAA"); it.newLine() }),
                Column(4, MeasurableWidget { it.writeLine("BBBB") }),
                Column(4, MeasurableWidget { it.writeLine("CCCC") }),
            ),
            gutter = 1,
        )

        val layout = columns.prepare(9)
        val buffer = render(layout, layout.width, layout.height)

        assertEquals(9, layout.width)
        assertEquals(4, layout.height)
        assertEquals("AAAA BBBB", buffer.line(0, width = 9))
        assertEquals("CCCC", buffer.line(3, width = 4))
    }

    @Test
    fun `nested blocks contribute padding and border extents`() {
        val content = MeasurableWidget { it.writeLine("X") }
            .block(Block(padding = Padding(left = 1, top = 2, right = 1, bottom = 1)))
            .block(Block.bordered(padding = Padding(left = 1, right = 1)))

        val layout = content.prepare(10)
        val buffer = render(layout, layout.width, layout.height)

        assertEquals(10, layout.width)
        assertEquals(6, layout.height)
        assertEquals("X", buffer.get(3, 3).symbol)
        assertEquals("╭", buffer.get(0, 0).symbol)
        assertEquals("╯", buffer.get(9, 5).symbol)
    }

    @Test
    fun `negative composition sizes are rejected`() {
        assertThrows<IllegalArgumentException> { Stack(emptyList(), gutter = -1) }
        assertThrows<IllegalArgumentException> {
            Columns(listOf(Column(-1, MeasurableWidget { })))
        }
        assertThrows<IllegalArgumentException> {
            io.archinaut.tenter.screen.Padding(left = -1)
        }
    }

    @Test
    fun `prepared viewport has exact long-content extent and owns settled snapshot`() {
        val state = ViewportState()
        val viewport = Viewport(MeasurableWidget { writer ->
            repeat(600) { writer.writeLine("row$it") }
        })
        val buffer = Buffer(10, 10)

        viewport.render(Canvas.of(buffer), state)

        assertEquals(590, state.settled?.maxOffset?.y)
        assertEquals(10, state.settled?.viewport?.height)
        assertEquals(600, state.settled?.content?.height)
        assertEquals("row0", buffer.line(0, width = 10))

        state.scrollBy(0, 590)
        assertEquals("row0", buffer.line(0, width = 10))

        viewport.render(Canvas.of(buffer), state)

        assertEquals(590, state.settled?.offset?.y)
        assertEquals("row590", buffer.line(0, width = 10))
    }

    @Test
    fun `manual scroll survives redraw while resize follows the same reveal`() {
        val state = ViewportState()
        val viewport = Viewport(MeasurableWidget { writer ->
            repeat(20) {
                writer.writeLine("row$it")
                if (it == 19) writer.markReveal(row = it)
            }
        })

        viewport.render(Canvas.offscreen(10, 5), state)
        state.scrollBy(0, -100)
        viewport.render(Canvas.offscreen(10, 5), state)
        assertEquals(0, state.settled?.offset?.y)

        viewport.render(Canvas.offscreen(10, 10), state)

        assertEquals(10, state.settled?.offset?.y)
        assertTrue(19 in state.settled!!.offset.y until state.settled!!.offset.y + 10)
    }

    @Test
    fun `recenter is queued and snapshot remains unchanged until a drawable frame`() {
        val state = ViewportState()
        val viewport = Viewport(MeasurableWidget { writer ->
            repeat(20) { writer.writeLine("row$it") }
            writer.markReveal(row = 10)
        })
        viewport.render(Canvas.offscreen(10, 5), state)
        val before = state.settled

        state.scrollBy(0, -100)
        state.requestRecenter()
        assertEquals(before, state.settled)

        viewport.render(Canvas.offscreen(10, 5), state)

        assertEquals(8, state.settled?.offset?.y)
    }

    @Test
    fun `zero-sized destination preserves the last snapshot and queued scroll`() {
        val state = ViewportState()
        val viewport = Viewport(MeasurableWidget { writer -> repeat(20) { writer.writeLine("row$it") } })
        viewport.render(Canvas.offscreen(10, 5), state)
        val before = state.settled
        state.scrollBy(0, 3)

        viewport.render(Canvas.offscreen(10, 0), state)

        assertEquals(before, state.settled)
        viewport.render(Canvas.offscreen(10, 5), state)
        assertEquals(3, state.settled?.offset?.y)
    }

    @Test
    fun `fixed prepared content scrolls in both axes`() {
        val fixed = object : Widget {
            override fun render(canvas: Canvas) {
                canvas.set(12, 5, Cell("X"))
            }
        }.fixedSize(20, 10)
        val state = ViewportState(Point(10, 4))
        val viewport = Viewport(fixed)
        val buffer = Buffer(10, 4)

        viewport.render(Canvas.of(buffer), state)

        assertEquals(10, state.settled?.maxOffset?.x)
        assertEquals(6, state.settled?.maxOffset?.y)
        assertEquals("X", buffer.get(2, 1).symbol)
    }

    @Test
    fun `inner viewport consumes its reveal instead of moving the outer viewport`() {
        val inner = Viewport(
            MeasurableWidget { writer ->
                repeat(20) { writer.writeLine("inner$it") }
                writer.markReveal(row = 19)
            },
        )
        val outerState = ViewportState()
        val outer = Viewport(inner.bind(ViewportState()).fixedSize(10, 20))

        outer.render(Canvas.offscreen(10, 5), outerState)

        assertNull(outerState.settled?.revealed)
        assertEquals(0, outerState.settled?.offset?.y)
    }

    @Test
    fun `prepared and fixed reveal targets follow composition order`() {
        val prepared = contentLayout(10, 1) {
            reveal(Rect(0, 0, 10, 1))
        }
        val fixed = object : Widget {
            override fun render(canvas: Canvas) {
                canvas.markReveal(Rect(0, 0, 10, 1))
            }
        }.fixedSize(10, 1).prepare(10)
        val fixedLast = object : LayoutWidget {
            override fun layout(availableWidth: Int): ContentLayout = contentLayout(10, 20) {
                place(0, 0, prepared)
                place(0, 10, fixed)
            }
        }
        val preparedLast = object : LayoutWidget {
            override fun layout(availableWidth: Int): ContentLayout = contentLayout(10, 20) {
                place(0, 10, fixed)
                place(0, 0, prepared)
            }
        }

        val fixedState = ViewportState()
        Viewport(fixedLast).render(Canvas.offscreen(10, 5), fixedState)
        val preparedState = ViewportState()
        Viewport(preparedLast).render(Canvas.offscreen(10, 5), preparedState)

        assertEquals(10, fixedState.settled?.revealed?.y)
        assertEquals(0, preparedState.settled?.revealed?.y)
    }

    @Test
    fun `a reveal request survives block padding and is the target followed`() {
        val content = object : LayoutWidget {
            override fun layout(availableWidth: Int): ContentLayout = contentLayout(availableWidth, 20) {
                reveal(Rect(0, 2, availableWidth, 1))
            }
        }
        val state = ViewportState()
        val viewport = Viewport(content.block(Block(padding = Padding(top = 1))))

        viewport.render(Canvas.offscreen(10, 5), state)

        assertEquals(3, state.settled?.revealed?.y)
        assertEquals(0, state.settled?.offset?.y)
    }

    @Test
    fun `prepared layout and raw fixed child are each painted once per frame`() {
        var layoutCalls = 0
        var renderCalls = 0
        val content = object : LayoutWidget {
            override fun layout(availableWidth: Int): ContentLayout {
                layoutCalls++
                return contentLayout(availableWidth, 2) {
                    place(0, 0, object : Widget {
                        override fun render(canvas: Canvas) {
                            renderCalls++
                            canvas.setString(0, 0, "X")
                        }
                    }.fixedSize(availableWidth, 2).prepare(availableWidth))
                }
            }
        }
        val viewport = Viewport(content)

        viewport.render(Canvas.offscreen(10, 5), ViewportState())

        assertEquals(1, layoutCalls)
        assertEquals(1, renderCalls)
    }

    private fun render(layout: ContentLayout, width: Int, height: Int): Buffer {
        val buffer = Buffer(width, height)
        layout.render(Canvas.of(buffer))
        return buffer
    }
}
