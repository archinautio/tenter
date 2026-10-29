// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Buffer

internal class ContentLayoutTest {

    @Test
    fun `raw layouts retain the widget while captured frame data stays repeatable`() {
        var label = "before"
        val captured = label
        val snapshot = object : Widget {
            public override fun render(canvas: Canvas) { canvas.setString(0, 0, captured) }
        }.fixedSize(8, 1).prepare(8)
        val live = object : Widget {
            public override fun render(canvas: Canvas) { canvas.setString(0, 0, label) }
        }.fixedSize(8, 1).prepare(8)
        label = "after"

        repeat(2) {
            val buffer = Buffer(8, 1)
            snapshot.render(Canvas.of(buffer))
            assertEquals("before", buffer.line(0))
            val liveBuffer = Buffer(8, 1)
            live.render(Canvas.of(liveBuffer))
            assertEquals("after", liveBuffer.line(0))
        }
    }

    @Test
    fun `flowing content counts trailing blank rows and blank-only content`() {
        val content = MeasurableWidget { writer ->
            writer.writeLine("line")
            writer.newLine()
            writer.newLine()
        }

        val layout = content.prepare(12)

        assertEquals(12, layout.width)
        assertEquals(3, layout.height)
        assertEquals(1, MeasurableWidget { it.newLine() }.measure(12).height)
        assertEquals(
            6,
            MeasurableWidget { writer -> writer.write(column = 0, text = "overlay", row = 5) }.measure(12).height,
        )
    }

    @Test
    fun `flowing content prepares a thousand rows without a height ceiling`() {
        val content = MeasurableWidget { writer ->
            repeat(1_000) { row -> writer.writeLine("row$row") }
        }

        val layout = content.prepare(20)
        val buffer = Buffer(layout.width, layout.height)
        layout.render(Canvas.of(buffer))

        assertEquals(1_000, layout.height)
        assertEquals("row999", buffer.line(999, width = 7))
    }

    @Test
    fun `zero available width remains valid and fixed content keeps its declared width`() {
        val flowing = MeasurableWidget { writer -> writer.writeLine("ignored") }
        val fixed = EmptyWidget.fixedSize(20, 4)

        assertEquals(0, flowing.measure(0).width)
        assertEquals(1, flowing.measure(0).height)
        assertEquals(20, fixed.measure(0).width)
        assertEquals(4, fixed.measure(0).height)
    }

    @Test
    fun `prepared text paints immutable recorded values`() {
        var label = "before"
        val content = MeasurableWidget { writer -> writer.writeLine(label) }

        val layout = content.prepare(20)
        label = "after"
        val buffer = Buffer(20, 1)
        layout.render(Canvas.of(buffer))

        assertEquals("before", buffer.line(0))
    }

    @Test
    fun `builder snapshot is not changed by later builder operations`() {
        lateinit var builder: ContentLayout.Builder
        val layout = contentLayout(8, 1) {
            builder = this
            request(VisibilityRequest.Reveal(Rect(0, 0, 1, 1)))
        }
        builder.request(VisibilityRequest.Reveal(Rect(7, 0, 1, 1)))

        assertEquals(Rect(0, 0, 1, 1), layout.request()?.area)
    }

    @Test
    fun `fixed content does not render its raw widget while preparing`() {
        var drawCount = 0
        val raw = object : Widget {
            override fun render(canvas: Canvas) {
                drawCount++
                canvas.setString(0, 0, "raw")
            }
        }
        val content = raw.fixedSize(8, 3)

        val layout = content.prepare(2)

        assertEquals(0, drawCount)
        assertEquals(8, layout.width)
        assertEquals(3, layout.height)

        layout.render(Canvas.offscreen(8, 3))
        layout.render(Canvas.offscreen(8, 3))
        assertEquals(2, drawCount)
    }

    @Test
    fun `prepared child is painted once per layout paint and does not append during painting`() {
        var prepareCount = 0
        val child = MeasurableWidget { writer ->
            prepareCount++
            writer.writeLine("child")
        }.revealed()
        val parent = MeasurableWidget { writer -> writer.draw(child) }

        val layout = parent.prepare(10)
        assertEquals(1, prepareCount)

        layout.render(Canvas.offscreen(10, 1))
        layout.render(Canvas.offscreen(10, 1))

        assertEquals(1, prepareCount)
    }

    @Test
    fun `row writer draw content uses the prepared logical height`() {
        val child = MeasurableWidget { content ->
            content.writeLine("A")
            content.newLine()
        }
        var rowAfter = 0
        val parent = MeasurableWidget { writer ->
            writer.draw(child)
            rowAfter = writer.row
        }

        val buffer = render(parent, 10, 5)

        assertEquals(2, rowAfter)
        assertEquals("A", buffer.get(0, 0).symbol)
    }

    @Test
    fun `custom layout places child and clips translated reveal to both rectangles`() {
        val child = contentLayout(5, 4) {
            request(VisibilityRequest.Reveal(Rect(x = -2, y = 1, width = 6, height = 3)))
            place(x = 0, y = 0, content = MeasurableWidget { it.writeLine("child") }.prepare(5))
        }
        val parent = contentLayout(4, 3) {
            place(x = -1, y = 1, content = child)
        }
        val buffer = Buffer(4, 3)
        val canvas = Canvas.of(buffer)

        parent.render(canvas)

        assertEquals(Rect(0, 2, 3, 1), parent.request()?.area)
        assertEquals("hild", buffer.line(1, width = 4))
    }

    @Test
    fun `the last request wins among children in declaration order`() {
        val top = contentLayout(4, 2) { request(VisibilityRequest.Reveal(Rect(0, 0, 4, 1))) }
        val bottom = EmptyWidget.fixedSize(4, 2).revealed(Rect(0, 1, 4, 1)).prepare(4)

        val bottomLast = contentLayout(4, 2) {
            place(0, 0, top)
            place(0, 0, bottom)
        }
        val topLast = contentLayout(4, 2) {
            place(0, 0, bottom)
            place(0, 0, top)
        }

        assertEquals(Rect(0, 1, 4, 1), bottomLast.request()?.area)
        assertEquals(Rect(0, 0, 4, 1), topLast.request()?.area)
    }

    @Test
    fun `a later child without a request keeps the earlier child's request`() {
        val target = contentLayout(4, 1) { request(VisibilityRequest.Reveal(Rect(0, 0, 4, 1))) }
        val noTarget = contentLayout(4, 1) { }
        val parent = contentLayout(4, 1) {
            place(0, 0, target)
            place(0, 0, noTarget)
        }

        assertEquals(Rect(0, 0, 4, 1), parent.request()?.area)
    }

    @Test
    fun `a plain widget makes no request until one is declared for it`() {
        val plain = EmptyWidget.fixedSize(5, 1)

        assertEquals(null, plain.prepare(5).request())
        assertEquals(Rect(0, 0, 1, 1), plain.revealed(Rect(0, 0, 1, 1)).prepare(5).request()?.area)
    }

    @Test
    fun `a child's wide glyph is clipped at its own edge without touching its neighbour`() {
        val columns = Flow(
            children = listOf(
                Constraint.Length(3) to MeasurableWidget { it.write(0, "ab中") },
                Constraint.Length(1) to MeasurableWidget { it.writeLine("z") },
            ),
            spacing = 0,
        )

        assertEquals("ab z", render(columns, 4, 1).line(0))
    }

    @Test
    fun `drawing a child without a request keeps an earlier child's request`() {
        val parent = MeasurableWidget { writer ->
            writer.draw(Paragraph("child").revealed())
            writer.draw(Paragraph("plain"))
        }

        assertEquals(Rect(0, 0, 6, 1), parent.prepare(6).request()?.area)
    }

    @Test
    fun `a viewport invokes a plain widget once per frame and follows the area declared for it`() {
        var paints = 0
        val raw = object : Widget {
            override fun render(canvas: Canvas) {
                paints++
                canvas.setString(0, 15, "here")
            }
        }.fixedSize(5, 20).revealed(Rect(0, 15, 5, 1))
        val state = ViewportState()

        val buffer = render(Viewport(raw), 5, 4, state)

        assertEquals(1, paints)
        assertEquals(12, checkNotNull(state.settled).offset.y)
        assertEquals("here", buffer.line(3))
    }
}
