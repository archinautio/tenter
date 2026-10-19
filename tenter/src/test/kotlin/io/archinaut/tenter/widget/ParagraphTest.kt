// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style

internal class ParagraphTest {

    @Test
    fun `splits a string into lines and keeps empty lines as blank rows`() {
        val paragraph = Paragraph("first\n\nthird", Style(ChromeRole.INFO))

        val buffer = render(paragraph, 10, 4)

        assertEquals(Size(10, 3), paragraph.measure(10))
        assertEquals(listOf("first", "", "third"), (0 until 3).map { buffer.line(it) })
        assertEquals(ChromeRole.INFO, buffer.get(0, 2).style.fg)
    }

    @Test
    fun `without wrap a long line takes one row and is ellipsized`() {
        val paragraph = Paragraph("the quick brown fox")

        assertEquals(1, paragraph.measure(8).height)
        assertEquals("the qui…", render(paragraph, 8, 2).line(0))
    }

    @Test
    fun `with wrap a long line wraps at word boundaries`() {
        val paragraph = Paragraph("the quick brown fox", wrap = true)

        val buffer = render(paragraph, 9, 4)

        assertEquals(2, paragraph.measure(9).height)
        assertEquals(listOf("the quick", "brown fox"), listOf(buffer.line(0), buffer.line(1)))
    }

    @Test
    fun `wrapping keeps each span's style and an empty line stays a blank row`() {
        val paragraph = Paragraph(
            listOf(Line(Span("key ", Style(ChromeRole.INFO)), Span("description")), Line("")),
            wrap = true,
        )

        val buffer = render(paragraph, 20, 3)

        assertEquals(2, paragraph.measure(20).height)
        assertEquals(ChromeRole.INFO, buffer.get(0, 0).style.fg)
        assertEquals(Style.DEFAULT, buffer.get(4, 0).style)
    }

    @Test
    fun `a paragraph takes a block like any measurable widget`() {
        val framed = Paragraph("hi").block(Block.bordered())

        assertEquals(Size(6, 3), framed.measure(6))
        assertEquals("│hi  │", render(framed, 6, 3).line(1))
    }
}
