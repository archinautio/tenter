// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Style

internal class ContentLayoutTest {

    @Test
    fun `raw layouts retain the widget while captured frame data stays repeatable`() {
        var label = "before"
        val captured = label
        val snapshot = fixedContent(8, 1, object : Widget {
            public override fun render(canvas: Canvas) { canvas.setString(0, 0, captured) }
        }).layout(8)
        val live = fixedContent(8, 1, object : Widget {
            public override fun render(canvas: Canvas) { canvas.setString(0, 0, label) }
        }).layout(8)
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
        val content = contentWidget { cursor ->
            cursor.writeLine("line")
            cursor.newLine()
            cursor.newLine()
        }

        val layout = content.layout(12)

        assertEquals(12, layout.width)
        assertEquals(3, layout.height)
        assertEquals(1, contentWidget { it.newLine() }.layout(12).height)
        assertEquals(
            6,
            contentWidget { cursor -> cursor.writeAt(column = 0, atRow = 5, text = "overlay") }.layout(12).height,
        )
    }

    @Test
    fun `flowing content prepares a thousand rows without a height ceiling`() {
        val content = contentWidget { cursor ->
            repeat(1_000) { row -> cursor.writeLine("row$row") }
        }

        val layout = content.layout(20)
        val buffer = Buffer(layout.width, layout.height)
        layout.render(Canvas.of(buffer))

        assertEquals(1_000, layout.height)
        assertEquals("row999", buffer.line(999, width = 7))
    }

    @Test
    fun `zero available width remains valid and fixed content keeps its declared width`() {
        val flowing = contentWidget { cursor -> cursor.writeLine("ignored") }
        val fixed = fixedContent(width = 20, height = 4, widget = EmptyWidget)

        assertEquals(0, flowing.layout(0).width)
        assertEquals(1, flowing.layout(0).height)
        assertEquals(20, fixed.layout(0).width)
        assertEquals(4, fixed.layout(0).height)
    }

    @Test
    fun `prepared text paints immutable recorded values`() {
        var label = "before"
        val content = contentWidget { cursor -> cursor.writeLine(label) }

        val layout = content.layout(20)
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
            reveal(Rect(0, 0, 1, 1))
        }
        builder.reveal(Rect(7, 0, 1, 1))
        val canvas = Canvas.offscreen(8, 1)

        layout.render(canvas)

        assertEquals(Rect(0, 0, 1, 1), canvas.revealRect())
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
        val content = fixedContent(width = 8, height = 3, widget = raw)

        val layout = content.layout(2)

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
        val child = object : ContentWidget {
            override fun layout(availableWidth: Int): ContentLayout {
                prepareCount++
                return contentLayout(availableWidth, 1) { reveal(Rect(0, 0, 1, 1)) }
            }
        }
        val parent = contentWidget { cursor -> cursor.draw(child) }

        val layout = parent.layout(10)
        assertEquals(1, prepareCount)

        layout.render(Canvas.offscreen(10, 1))
        layout.render(Canvas.offscreen(10, 1))

        assertEquals(1, prepareCount)
    }

    @Test
    fun `text cursor draw content uses the prepared logical height`() {
        val canvas = Canvas.offscreen(10, 5)
        val cursor = TextCursor(canvas)
        val child = contentWidget { content ->
            content.writeLine("A")
            content.newLine()
        }

        val used = cursor.draw(child)

        assertEquals(2, used)
        assertEquals(2, cursor.row)
        assertEquals("A", canvas.get(0, 0).symbol)
    }

    @Test
    fun `custom layout places child and clips translated reveal to both rectangles`() {
        val child = contentLayout(5, 4) {
            reveal(Rect(x = -2, y = 1, width = 6, height = 3))
            place(x = 0, y = 0, content = contentWidget { it.writeLine("child") }.layout(5))
        }
        val parent = contentLayout(4, 3) {
            place(x = -1, y = 1, content = child)
        }
        val buffer = Buffer(4, 3)
        val canvas = Canvas.of(buffer)

        parent.render(canvas)

        assertEquals(Rect(0, 2, 3, 1), canvas.revealRect())
        assertEquals("hild", buffer.line(1, width = 4))
    }

    @Test
    fun `last and first preferences resolve prepared and raw requests in declaration order`() {
        val prepared = contentLayout(4, 2) { reveal(Rect(0, 0, 4, 1)) }
        val raw = fixedContent(
            width = 4,
            height = 2,
            widget = object : Widget {
                override fun render(canvas: Canvas) {
                    canvas.markReveal(Rect(0, 1, 4, 1))
                }
            },
        ).layout(4)

        val last = contentLayout(4, 2, RevealPreference.LAST) {
            place(0, 0, prepared)
            place(0, 0, raw)
        }
        val first = contentLayout(4, 2, RevealPreference.FIRST) {
            place(0, 0, prepared)
            place(0, 0, raw)
        }

        val lastCanvas = Canvas.offscreen(4, 2)
        val firstCanvas = Canvas.offscreen(4, 2)
        last.render(lastCanvas)
        first.render(firstCanvas)

        assertEquals(Rect(0, 1, 4, 1), lastCanvas.revealRect())
        assertEquals(Rect(0, 0, 4, 1), firstCanvas.revealRect())

        val reverseLast = contentLayout(4, 2, RevealPreference.LAST) {
            place(0, 0, raw)
            place(0, 0, prepared)
        }
        val reverseFirst = contentLayout(4, 2, RevealPreference.FIRST) {
            place(0, 0, raw)
            place(0, 0, prepared)
        }
        val reverseLastCanvas = Canvas.offscreen(4, 2)
        val reverseFirstCanvas = Canvas.offscreen(4, 2)
        reverseLast.render(reverseLastCanvas)
        reverseFirst.render(reverseFirstCanvas)

        assertEquals(Rect(0, 0, 4, 1), reverseLastCanvas.revealRect())
        assertEquals(Rect(0, 1, 4, 1), reverseFirstCanvas.revealRect())
    }

    @Test
    fun `nested preference resolves inside the child before parent composition`() {
        val child = contentLayout(4, 3, RevealPreference.FIRST) {
            reveal(Rect(0, 0, 4, 1))
            reveal(Rect(0, 2, 4, 1))
        }
        val parent = contentLayout(4, 3) { place(0, 0, child) }
        val canvas = Canvas.offscreen(4, 3)

        parent.render(canvas)

        assertEquals(Rect(0, 0, 4, 1), canvas.revealRect())
    }

    @Test
    fun `first preference falls back when the first child has no target`() {
        val noTarget = contentLayout(4, 1) { }
        val target = contentLayout(4, 1) { reveal(Rect(0, 0, 4, 1)) }
        val parent = contentLayout(4, 1, RevealPreference.FIRST) {
            place(0, 0, noTarget)
            place(0, 0, target)
        }
        val canvas = Canvas.offscreen(4, 1)

        parent.render(canvas)

        assertEquals(Rect(0, 0, 4, 1), canvas.revealRect())
    }

    @Test
    fun `painting a layout with no request clears a stale canvas reveal`() {
        val buffer = Buffer(4, 2)
        val canvas = Canvas.of(buffer)
        val revealing = contentLayout(4, 2) { reveal(Rect(0, 1, 4, 1)) }
        val quiet = contentLayout(4, 2) { }

        revealing.render(canvas)
        assertTrue(canvas.revealRect() != null)
        quiet.render(canvas)

        assertFalse(canvas.revealRect() != null)
    }

    @Test
    fun `fixed content preserves raw paint order and reveal`() {
        val content = fixedContent(
            width = 5,
            height = 1,
            widget = object : Widget {
                override fun render(canvas: Canvas) {
                    canvas.set(0, 0, Cell("X", Style(fg = ChromeRole.SUCCESS)))
                    canvas.markReveal(Rect(0, 0, 1, 1))
                }
            },
        )
        val canvas = Canvas.offscreen(5, 1)

        content.render(canvas)

        assertEquals("X", canvas.get(0, 0).symbol)
        assertEquals(Rect(0, 0, 1, 1), canvas.revealRect())
    }

    @Test
    fun `a child's wide glyph is clipped at its own edge without touching its neighbour`() {
        val columns = Columns(
            children = listOf(
                Columns.Child(3, contentWidget { it.write(0, "ab中") }),
                Columns.Child(1, contentWidget { it.writeLine("z") }),
            ),
            gutter = 0,
        )

        assertEquals("ab z", render(columns, 4, 1).line(0))
    }

    @Test
    fun `immediate cursor drawing keeps an earlier reveal when the child has none`() {
        val canvas = Canvas.offscreen(6, 3)
        val cursor = TextCursor(canvas)

        cursor.markReveal()
        cursor.draw(contentWidget { it.writeLine("child") })

        assertEquals(Rect(0, 0, 6, 1), canvas.revealRect())
        assertEquals("child", (0 until 5).joinToString("") { canvas.get(it, 0).symbol })
    }

    @Test
    fun `a viewport invokes a raw widget once per frame and follows its reveal`() {
        var paints = 0
        val raw = fixedContent(5, 20, object : Widget {
            override fun render(canvas: Canvas) {
                paints++
                canvas.setString(0, 15, "here")
                canvas.markReveal(Rect(0, 15, 5, 1))
            }
        })
        val state = ViewportState()

        val buffer = render(Viewport(raw), 5, 4, state)

        assertEquals(1, paints)
        assertEquals(12, checkNotNull(state.settled).offset.y)
        assertEquals("here", buffer.line(3))
    }
}
