// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import com.github.ajalt.mordant.rendering.TextAlign
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style

internal class ParagraphTest {

    @Test
    fun `splits a string into lines and keeps empty lines as blank rows`() {
        val paragraph = Paragraph("first\n\nthird", Style(StandardRole.INFO))

        val buffer = render(paragraph, 10, 4)

        assertEquals(Size(10, 3), paragraph.measure(10))
        assertEquals(listOf("first", "", "third"), (0 until 3).map { buffer.line(it) })
        assertEquals(StandardRole.INFO, buffer.get(0, 2).fg)
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
            listOf(Line(Span("key ", Style(StandardRole.INFO)), Span("description")), Line("")),
            wrap = true,
        )

        val buffer = render(paragraph, 20, 3)

        assertEquals(2, paragraph.measure(20).height)
        assertEquals(StandardRole.INFO, buffer.get(0, 0).fg)
        assertEquals(StandardRole.DEFAULT, buffer.get(4, 0).fg)
    }

    @Test
    fun `a paragraph takes a block like any measurable widget`() {
        val framed = Paragraph("hi").block(Block.bordered())

        assertEquals(Size(6, 3), framed.measure(6))
        assertEquals("│hi  │", render(framed, 6, 3).line(1))
    }

    @Test
    fun `centers and right-aligns each row within the width`() {
        assertEquals("  ab", render(Paragraph("ab", align = TextAlign.CENTER), 6, 1).line(0))
        assertEquals("    ab", render(Paragraph("ab", align = TextAlign.RIGHT), 6, 1).line(0))
    }

    @Test
    fun `centering leaves the odd cell on the right`() {
        assertEquals(" ab", render(Paragraph("ab", align = TextAlign.CENTER), 5, 1).line(0))
    }

    @Test
    fun `aligns each wrapped row on its own`() {
        val paragraph = Paragraph("the quick fox", wrap = true, align = TextAlign.RIGHT)

        val buffer = render(paragraph, 9, 2)

        assertEquals(listOf("the quick", "      fox"), listOf(buffer.line(0), buffer.line(1)))
    }

    @Test
    fun `a line too wide for any alignment is ellipsized at its end`() {
        val buffer = render(Paragraph("the quick brown fox", align = TextAlign.RIGHT), 8, 1)

        assertEquals("the qui…", buffer.line(0))
    }

    @Test
    fun `justify fills every wrapped row but a line's last`() {
        val paragraph = Paragraph("a bc de fgh ij", wrap = true, align = TextAlign.JUSTIFY)

        val buffer = render(paragraph, 9, 2)

        assertEquals(listOf("a  bc  de", "fgh ij"), listOf(buffer.line(0), buffer.line(1)))
    }

    @Test
    fun `an unwrapped justified paragraph stays flush left`() {
        assertEquals("a b", render(Paragraph("a b", align = TextAlign.JUSTIFY), 9, 1).line(0))
    }

    @Test
    fun `none aligns flush left`() {
        assertEquals("ab", render(Paragraph("ab", align = TextAlign.NONE), 6, 1).line(0))
    }
}
