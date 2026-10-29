// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.ScrollState
import io.archinaut.tenter.widget.Scrollbar
import io.archinaut.tenter.widget.bind
import io.archinaut.tenter.widget.line
import io.archinaut.tenter.widget.render

/**
 * What a panel paints around its content: a bordered frame, a scrolling viewport inside it, and
 * scrollbar thumbs on its borders. The top padding row is part of the *content stream* rather
 * than the viewport, which is what makes it a spacer at rest that the content reclaims the instant
 * it scrolls.
 */
internal class PanelFrameTest {

    private enum class Id : PanelId { ONLY }

    private fun stubContent(lines: Int): MeasurableWidget = MeasurableWidget { writer ->
        repeat(lines) { row -> writer.writeLine("line$row") }
    }

    private var panels: PanelSet<Id, Unit>? = null

    /** Draws a single panel showing [content] into a [width] by [height] frame, scrolled down by [scrollOffset]. */
    private fun draw(content: MeasurableWidget, width: Int = 30, height: Int = 10, scrollOffset: Int = 0): Buffer {
        val set = PanelSet(listOf(Panel<Id, Unit>(Id.ONLY, title = "T", badge = "0", normal = { content })))
        panels = set
        set.scroll(Id.ONLY, 0, scrollOffset)
        return render(Panels(Unit, mapOf(Id.ONLY to Rect(0, 0, width, height))).bind(set), width, height)
    }

    /** The settled frame of the last [draw], or null when it had no drawable viewport. */
    private fun settled(): ScrollState? = checkNotNull(panels).layout?.slotOf(Id.ONLY)?.scroll

    @Test
    fun `content at offset 0 starts at row y+2 col x+2`() {
        val buffer = draw(stubContent(5))

        assertEquals("line0", buffer.line(2, 2, 10))
        assertEquals("line1", buffer.line(3, 2, 10))
    }

    @Test
    fun `explicit offset shifts the visible window`() {
        val buffer = draw(stubContent(20), scrollOffset = 3)

        // Row 1 is the viewport's first row now that the padding scrolls with the stream.
        assertEquals("line2", buffer.line(1, 2, 10))
        assertEquals("line3", buffer.line(2, 2, 10))
        assertEquals("line4", buffer.line(3, 2, 10))
    }

    @Test
    fun `top padding shows at rest and is reclaimed once content scrolls`() {
        val atRest = draw(stubContent(20))
        assertEquals("", atRest.line(1, 2, 10))
        assertEquals("line0", atRest.line(2, 2, 10))
        assertEquals("line6", atRest.line(8, 2, 10))

        val scrolled = draw(stubContent(20), scrollOffset = 1)
        // Content reclaims the padding row: line0 moves up into row 1...
        assertEquals("line0", scrolled.line(1, 2, 10))
        // ...and one more line becomes visible at the bottom than was shown at rest.
        assertEquals("line7", scrolled.line(8, 2, 10))
    }

    @Test
    fun `offset beyond maxOffset is clamped to maxOffset`() {
        val buffer = draw(stubContent(5), scrollOffset = 999)

        val viewportHeight = 8
        val streamHeight = 5 + 1 // the reclaimable top-padding row is prepended to the stream
        val maxOffset = maxOf(0, streamHeight - viewportHeight)
        assertEquals("line$maxOffset", buffer.line(2, 2, 10))
    }

    @Test
    fun `maxOffset accounts for the reclaimable top-padding row`() {
        draw(stubContent(20))

        val viewportHeight = 8
        val streamHeight = 20 + 1 // the reclaimable top-padding row is prepended to the stream
        assertEquals(streamHeight - viewportHeight, checkNotNull(settled()).maxOffset.y)
    }

    @Test
    fun `scrollbar thumb appears on the right border only where it sits`() {
        val buffer = draw(stubContent(20))

        // The viewport spans the whole inner height (rows 1..8) — the padding row is part of the
        // scrollable stream, not a fixed offset — so the thumb can occupy row 1 too.
        val thumbRange = Scrollbar.thumb(track = 8, contentLength = 21, viewportLength = 8, offset = 0)!!
        for (row in 1..8) {
            val expected = if ((row - 1) in thumbRange) "▐" else "│"
            assertEquals(expected, buffer.get(29, row).symbol, "row $row")
        }
    }

    @Test
    fun `no scrollbar cells when content exactly fits viewport`() {
        // The stream includes one extra row for the reclaimable top padding, so 7 content lines
        // exactly fill the 8-row viewport with no overflow.
        val buffer = draw(stubContent(7))

        for (row in 1..8) {
            assertEquals("│", buffer.get(29, row).symbol, "expected plain border at row $row")
        }
    }

    @Test
    fun `scrollbar thumb uses the frame's own border color`() {
        val buffer = draw(stubContent(20))

        val thumbRow = 1 + Scrollbar.thumb(track = 8, contentLength = 21, viewportLength = 8, offset = 0)!!.first
        assertEquals(StandardRole.PANEL_BORDER_FOCUSED, buffer.get(29, 0).style.fg)
        assertEquals(StandardRole.PANEL_BORDER_FOCUSED, buffer.get(29, thumbRow).style.fg)
    }

    @Test
    fun `degenerate height 2 draws only box and never throws`() {
        val buffer = draw(stubContent(5), height = 2)

        assertEquals("╭", buffer.get(0, 0).symbol)
        assertEquals("╰", buffer.get(0, 1).symbol)
        assertNull(settled(), "a destination this small has no drawable viewport")
    }

    @Test
    fun `degenerate width 4 draws only box and never throws`() {
        val buffer = draw(stubContent(5), width = 4)

        assertEquals("╭", buffer.get(0, 0).symbol)
        assertNull(settled(), "a destination this narrow has no drawable viewport")
    }
}
