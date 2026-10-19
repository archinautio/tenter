// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.palette.ChromeRole

/** Pixel parity for [Block]'s border, title, badge, padding, and scrollbar-thumb decoration. */
internal class BlockTest {

    private val blank: Widget = object : Widget {
        override fun render(canvas: Canvas) = Unit
    }

    @Test
    fun `renders corners around the whole canvas`() {
        val widget = Block()

        val buffer = render(widget, 10, 5)

        assertEquals("╭", buffer.get(0, 0).symbol)
        assertEquals("╮", buffer.get(9, 0).symbol)
        assertEquals("╰", buffer.get(0, 4).symbol)
        assertEquals("╯", buffer.get(9, 4).symbol)
    }

    @Test
    fun `renders horizontal and vertical borders`() {
        val widget = Block()

        val buffer = render(widget, 6, 4)

        for (i in 1..4) {
            assertEquals("─", buffer.get(i, 0).symbol)
            assertEquals("─", buffer.get(i, 3).symbol)
        }
        for (i in 1..2) {
            assertEquals("│", buffer.get(0, i).symbol)
            assertEquals("│", buffer.get(5, i).symbol)
        }
    }

    @Test
    fun `renders title`() {
        val widget = Block(title = "TEST")

        val buffer = render(widget, 20, 3)

        assertEquals(" ", buffer.get(3, 0).symbol)
        assertEquals("T", buffer.get(4, 0).symbol)
        assertEquals("E", buffer.get(5, 0).symbol)
        assertEquals("S", buffer.get(6, 0).symbol)
        assertEquals("T", buffer.get(7, 0).symbol)
        assertEquals(" ", buffer.get(8, 0).symbol)
        assertEquals(ChromeRole.ACCENT, buffer.get(4, 0).style.fg)
        assertEquals(ChromeRole.PANEL_BORDER, buffer.get(0, 0).style.fg)
    }

    @Test
    fun `uses specified colors`() {
        val widget = Block(borderColor = ChromeRole.DANGER, titleColor = ChromeRole.TEXT_PRIMARY)

        val buffer = render(widget, 10, 3)

        assertEquals(ChromeRole.DANGER, buffer.get(0, 0).style.fg)
    }

    @Test
    fun `renders single-char badge title`() {
        val widget = Block(title = "TEST", badge = "2")

        val buffer = render(widget, 20, 3)

        assertEquals("[", buffer.get(2, 0).symbol)
        assertEquals("2", buffer.get(3, 0).symbol)
        assertEquals("]", buffer.get(4, 0).symbol)
        assertEquals(" ", buffer.get(5, 0).symbol)
        assertEquals("T", buffer.get(6, 0).symbol)
        assertEquals("E", buffer.get(7, 0).symbol)
        assertEquals("S", buffer.get(8, 0).symbol)
        assertEquals("T", buffer.get(9, 0).symbol)
        assertEquals(" ", buffer.get(10, 0).symbol)
        assertEquals(ChromeRole.ACCENT, buffer.get(2, 0).style.fg)
        assertEquals(ChromeRole.ACCENT, buffer.get(6, 0).style.fg)
    }

    @Test
    fun `renders letter badge title, e g HELP`() {
        val widget = Block(title = "HELP", badge = "h")

        val buffer = render(widget, 20, 3)

        assertEquals("[", buffer.get(2, 0).symbol)
        assertEquals("h", buffer.get(3, 0).symbol)
        assertEquals("]", buffer.get(4, 0).symbol)
        assertEquals(" ", buffer.get(5, 0).symbol)
        assertEquals("H", buffer.get(6, 0).symbol)
        assertEquals("E", buffer.get(7, 0).symbol)
        assertEquals("L", buffer.get(8, 0).symbol)
        assertEquals("P", buffer.get(9, 0).symbol)
        assertEquals(" ", buffer.get(10, 0).symbol)
        assertEquals(ChromeRole.ACCENT, buffer.get(3, 0).style.fg)
        assertEquals(ChromeRole.ACCENT, buffer.get(6, 0).style.fg)
    }

    @Test
    fun `skips title when box too narrow`() {
        val widget = Block(title = "TOOLONG")

        val buffer = render(widget, 8, 3)

        assertEquals("─", buffer.get(3, 0).symbol)
    }

    @Test
    fun `renders a badge alone, with no title to anchor it to — e g a minimized panel's stub`() {
        val widget = Block(badge = "0")

        val buffer = render(widget, 7, 3)

        assertEquals("[", buffer.get(2, 0).symbol)
        assertEquals("0", buffer.get(3, 0).symbol)
        assertEquals("]", buffer.get(4, 0).symbol)
    }

    @Test
    fun `omits a badge that has no room, even alone`() {
        val widget = Block(badge = "0")

        val buffer = render(widget, 5, 3)

        assertEquals("─", buffer.get(3, 0).symbol, "5-wide box has no room for [0] at column 2")
    }

    @Test
    fun `falls back to the badge alone when a title is present but the full run does not fit`() {
        // A title too long to fit as "[badge] title" must not draw nothing — the badge alone is
        // still worth showing (e.g. a minimized panel's stub, too narrow for its full title).
        val widget = Block(title = "TOOLONGTOFIT", badge = "1")

        val buffer = render(widget, 7, 3)

        assertEquals("[", buffer.get(2, 0).symbol)
        assertEquals("1", buffer.get(3, 0).symbol)
        assertEquals("]", buffer.get(4, 0).symbol)
        assertEquals(ChromeRole.ACCENT, buffer.get(3, 0).style.fg)
    }

    @Test
    fun `content renders inside the border, offset by padding`() {
        val content = object : Widget {
            override fun render(canvas: Canvas) {
                canvas.setString(0, 0, "HI")
            }
        }
        val block = Block(padding = Padding(left = 2, right = 2))
        val buffer = Buffer(20, 5)
        val canvas = Canvas.of(buffer)

        block.render(canvas)
        content.render(block.inner(canvas))

        assertEquals("H", buffer.get(3, 1).symbol)
        assertEquals("I", buffer.get(4, 1).symbol)
    }

    @Test
    fun `content rendered at offset position within parent buffer`() {
        val block = Block()
        val buffer = Buffer(40, 20)
        val canvas = Canvas.of(buffer).region(Rect(5, 3, 20, 10))

        block.render(canvas)
        block.inner(canvas).setString(0, 0, "X")

        assertEquals("╭", buffer.get(5, 3).symbol)
        assertEquals("X", buffer.get(6, 4).symbol)
    }

    // ── scrollbar thumbs, synchronized with a Viewport ─────────────────────────────────────────

    private fun stubContent(lines: Int): ContentWidget = contentWidget { cursor ->
        repeat(lines) { row -> cursor.writeLine("line$row") }
    }

    @Test
    fun `scrollbar block cells appear on right border only when content overflows`() {
        val widget = ScrollingPanel(title = "T", badge = "0", content = stubContent(20))

        val buffer = render(widget, 30, 10, ViewportState())

        // The viewport spans the whole inner height (rows 1..8) — the padding row is part of the
        // scrollable stream, not a fixed offset — so the thumb can occupy row 1 too.
        val thumbRange = ScrollGeometry.thumb(track = 8, contentLength = 21, viewportLength = 8, offset = 0)!!
        for (row in 1..8) {
            val cell = buffer.get(29, row)
            if ((row - 1) in thumbRange) {
                assertEquals("▐", cell.symbol, "expected thumb at row $row")
                assertEquals(ChromeRole.PANEL_BORDER, cell.style.fg)
            } else {
                assertEquals("│", cell.symbol, "expected border at row $row")
            }
        }
    }

    @Test
    fun `no scrollbar cells when content exactly fits viewport`() {
        val viewportHeight = 8
        // The stream includes one extra row for the reclaimable top padding, so
        // `viewportHeight - 1` content lines exactly fill it with no overflow.
        val widget = ScrollingPanel(title = "T", badge = "0", content = stubContent(viewportHeight - 1))

        val buffer = render(widget, 30, 10, ViewportState())

        for (row in 1..8) {
            assertEquals("│", buffer.get(29, row).symbol, "expected plain border at row $row")
        }
    }

    @Test
    fun `scrollbar thumb uses the box's own borderColor, not a hardcoded one`() {
        val widget = ScrollingPanel(
            title = "T",
            badge = "0",
            content = stubContent(20),
            borderColor = ChromeRole.PANEL_BORDER_FOCUSED,
        )

        val buffer = render(widget, 30, 10, ViewportState())

        val thumbRange = ScrollGeometry.thumb(track = 8, contentLength = 21, viewportLength = 8, offset = 0)!!
        val row = 1 + thumbRange.first
        assertEquals(ChromeRole.PANEL_BORDER_FOCUSED, buffer.get(29, row).style.fg)
    }

    @Test
    fun `a plain block never draws a thumb, however tall its content`() {
        val block = Block()
        val buffer = Buffer(30, 10)
        val canvas = Canvas.of(buffer)

        block.render(canvas)
        stubContent(20).render(block.inner(canvas))

        for (row in 1..8) {
            assertEquals("│", buffer.get(29, row).symbol, "expected plain border, no thumb without a Viewport")
        }
    }

    @Test
    fun `title placement uses display width rather than UTF-16 length`() {
        val widget = Block(title = "中")

        val buffer = render(widget, 12, 3)

        assertEquals("中", buffer.get(4, 0).symbol)
        assertEquals(" ", buffer.get(6, 0).symbol)
    }

    @Test
    fun `wide badge leaves the right border intact when shown alone`() {
        val widget = Block(badge = "中")

        val buffer = render(widget, 7, 3)

        assertEquals("[", buffer.get(2, 0).symbol)
        assertEquals("中", buffer.get(3, 0).symbol)
        assertEquals("", buffer.get(4, 0).symbol)
        assertEquals("]", buffer.get(5, 0).symbol)
        assertEquals("╮", buffer.get(6, 0).symbol)
    }

    @Test
    fun `inner removes the border and padding from an area`() {
        val block = Block(padding = Padding(left = 2, top = 1))

        assertEquals(Rect(13, 22, 6, 7), block.inner(Rect(10, 20, 10, 10)))
        assertEquals(Rect(2, 1, 0, 0), block.inner(Rect(0, 0, 2, 1)))
    }
}
