// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VerticalTextTest {

    @Test
    fun `draws no border cells — content only`() {
        val widget = VerticalText("LOG")

        val buffer = render(widget, 7, 10)

        val topRow = buffer.line(0, width = 7)
        assert(!topRow.contains("╭") && !topRow.contains("╮")) { "Should draw no border, got: $topRow" }
    }

    @Test
    fun `renders vertical title chars centred`() {
        val widget = VerticalText("LOG")

        val buffer = render(widget, 7, 10)

        val cx = 3 // centre for width=7
        assertEquals("L", buffer.get(cx, 0).symbol)
        assertEquals("O", buffer.get(cx, 1).symbol)
        assertEquals("G", buffer.get(cx, 2).symbol)
        assertEquals(" ", buffer.get(cx, 3).symbol)
    }

    @Test
    fun `renders a multi-word title with space as blank row`() {
        val widget = VerticalText("UNIT STATUS")

        val buffer = render(widget, 7, 15)

        val cx = 3
        assertEquals("U", buffer.get(cx, 0).symbol)
        assertEquals("N", buffer.get(cx, 1).symbol)
        assertEquals("I", buffer.get(cx, 2).symbol)
        assertEquals("T", buffer.get(cx, 3).symbol)
        // row 4 = space -> blank
        assertEquals(" ", buffer.get(cx, 4).symbol)
        assertEquals("S", buffer.get(cx, 5).symbol)
        assertEquals("T", buffer.get(cx, 6).symbol)
        assertEquals("A", buffer.get(cx, 7).symbol)
        assertEquals("T", buffer.get(cx, 8).symbol)
        assertEquals("U", buffer.get(cx, 9).symbol)
        assertEquals("S", buffer.get(cx, 10).symbol)
    }

    @Test
    fun `clips title that exceeds panel height without throwing`() {
        val widget = VerticalText("ABCDEFGHIJKLMNOPQRST")

        val buffer = render(widget, 7, 3)

        val cx = 3
        assertEquals("A", buffer.get(cx, 0).symbol)
        assertEquals("B", buffer.get(cx, 1).symbol)
        assertEquals("C", buffer.get(cx, 2).symbol)
    }

    @Test
    fun `uses one row per grapheme cluster`() {
        val buffer = render(VerticalText("e\u0301👩‍💻X"), 7, 4)

        assertEquals("e\u0301", buffer.get(3, 0).symbol)
        assertEquals("👩‍💻", buffer.get(3, 1).symbol)
        assertEquals("X", buffer.get(3, 2).symbol)
    }
}
