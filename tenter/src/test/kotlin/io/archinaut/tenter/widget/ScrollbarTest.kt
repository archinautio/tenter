// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Size
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class ScrollbarTest {

    @Test
    fun `returns null when content fits in viewport`() {
        assertNull(Scrollbar.thumb(track = 10, contentLength = 5, viewportLength = 10, offset = 0))
        assertNull(Scrollbar.thumb(track = 10, contentLength = 10, viewportLength = 10, offset = 0))
    }

    @Test
    fun `returns null for degenerate track of zero or less`() {
        assertNull(Scrollbar.thumb(track = 0, contentLength = 20, viewportLength = 5, offset = 0))
        assertNull(Scrollbar.thumb(track = -1, contentLength = 20, viewportLength = 5, offset = 0))
    }

    @Test
    fun `minimum thumb size is 1 for huge content`() {
        val range = Scrollbar.thumb(track = 10, contentLength = 10000, viewportLength = 10, offset = 0)!!
        assertEquals(1, range.last - range.first + 1)
    }

    @Test
    fun `thumb is flush at top when offset is 0`() {
        val range = Scrollbar.thumb(track = 10, contentLength = 20, viewportLength = 10, offset = 0)!!
        assertEquals(0, range.first)
    }

    @Test
    fun `thumb is flush at bottom when offset equals maxOffset`() {
        val maxOffset = 10
        val range = Scrollbar.thumb(track = 10, contentLength = 20, viewportLength = 10, offset = maxOffset)!!
        assertEquals(9, range.last)
    }

    @Test
    fun `thumb is proportional in the middle`() {
        val track = 10
        val contentLength = 20
        val viewportLength = 10
        val maxOffset = contentLength - viewportLength
        val thumbSize = track * viewportLength / contentLength

        val range = Scrollbar.thumb(track, contentLength, viewportLength, offset = maxOffset / 2)!!
        val expectedStart = (maxOffset / 2 * (track - thumbSize) + maxOffset / 2) / maxOffset
        assertEquals(expectedStart, range.first)
        assertEquals(expectedStart + thumbSize - 1, range.last)
    }

    @Test
    fun `track of 1 row clamps thumb to that single row`() {
        val range = Scrollbar.thumb(track = 1, contentLength = 20, viewportLength = 5, offset = 0)!!
        assertEquals(0, range.first)
        assertEquals(0, range.last)
    }

    @Test
    fun `a vertical bar paints its thumb down the first column of its track`() {
        val buffer = render(Scrollbar.vertical().bind(scroll(offset = Point(0, 10))), 1, 4)

        assertEquals(listOf(" ", " ", "▐", "▐"), (0 until 4).map { buffer.get(0, it).symbol })
    }

    @Test
    fun `a horizontal bar paints its thumb along the first row of its track`() {
        val buffer = render(Scrollbar.horizontal().bind(scroll(offset = Point(0, 0))), 4, 1)

        assertEquals("▬▬", buffer.line(0))
    }

    @Test
    fun `a bar paints nothing while the content fits`() {
        val fits = ScrollState(Point.ORIGIN, Point.ORIGIN, viewport = Size(4, 4), content = Size(4, 2))

        val buffer = render(Scrollbar.vertical().bind(fits), 1, 4)

        assertEquals("", buffer.text().trim())
    }

    /** A frame showing 10x10 of 20x20 content at [offset]. */
    private fun scroll(offset: Point): ScrollState =
        ScrollState(offset, maxOffset = Point(10, 10), viewport = Size(10, 10), content = Size(20, 20))
}
