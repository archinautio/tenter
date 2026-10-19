// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.palette.ChromeRole

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.text.TextWrap

internal class LineTest {

    @Test
    fun `adjacent same-styled spans merge`() {
        val text = Line(Span("foo", Style(ChromeRole.DANGER)), Span("bar", Style(ChromeRole.DANGER)))

        assertEquals(listOf(Span("foobar", Style(ChromeRole.DANGER))), text.spans)
    }

    @Test
    fun `empty spans are dropped`() {
        val text = Line(
            Span("foo", Style(ChromeRole.DANGER)),
            Span("", Style(ChromeRole.SUCCESS)),
            Span("bar", Style(ChromeRole.SUCCESS)),
        )

        assertEquals(
            listOf(Span("foo", Style(ChromeRole.DANGER)), Span("bar", Style(ChromeRole.SUCCESS))),
            text.spans,
        )
    }

    @Test
    fun `two values that paint identically are equal`() {
        val a = Line(Span("foo", Style(ChromeRole.DANGER)))
        val b = Line(Span("f", Style(ChromeRole.DANGER)), Span("oo", Style(ChromeRole.DANGER)))

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `plain concatenates every span's text`() {
        val text = Line(Span("Grasshopper", Style(ChromeRole.ACCENT)), Span(" destroyed"))

        assertEquals("Grasshopper destroyed", text.plain)
    }

    @Test
    fun `wrapping matches the flat wrap engine's line breaks`() {
        val text = Line(Span("The quick brown", Style(ChromeRole.ACCENT)), Span(" fox jumps over the lazy dog"))

        for (width in listOf(3, 5, 8, 12, 40)) {
            val expected = TextWrap.wrap(text.plain, width)
            val actual = text.wrap(width).map { it.plain }
            assertEquals(expected, actual, "width=$width")
        }
    }

    @Test
    fun `a span straddling a wrap point is sliced and each row keeps its style`() {
        val text = Line(Span("Grasshopper", Style(ChromeRole.ACCENT)), Span(" hits ", Style.DEFAULT), Span("Atlas", Style(ChromeRole.DANGER)))

        val rows = text.wrap(firstWidth = 11, continuationWidth = 10)

        assertEquals(listOf("Grasshopper", "hits Atlas"), rows.map { it.plain })
        assertEquals(listOf(Span("Grasshopper", Style(ChromeRole.ACCENT))), rows[0].spans)
        assertEquals(
            listOf(
                Span("hits ", Style.DEFAULT),
                Span("Atlas", Style(ChromeRole.DANGER)),
            ),
            rows[1].spans,
        )
    }

    @Test
    fun `an over-long word inside one colored span hard-breaks and every fragment keeps the style`() {
        val text = Line(Span("supercalifragilistic", Style(ChromeRole.DANGER)))

        val rows = text.wrap(8)

        assertEquals(listOf("supercal", "ifragili", "stic"), rows.map { it.plain })
        rows.forEach { row ->
            assertEquals(listOf(Style(ChromeRole.DANGER)), row.spans.map { it.style }.distinct())
        }
    }

    @Test
    fun `the separating space between two differently-styled words keeps the source's style`() {
        val text = Line(Span("foo", Style(ChromeRole.DANGER)), Span(" ", Style(ChromeRole.SUCCESS)), Span("bar", Style(ChromeRole.ACCENT)))

        val row = text.wrap(20).single()

        assertEquals(
            listOf(
                Span("foo", Style(ChromeRole.DANGER)),
                Span(" ", Style(ChromeRole.SUCCESS)),
                Span("bar", Style(ChromeRole.ACCENT)),
            ),
            row.spans,
        )
    }

    @Test
    fun `ellipsize keeps text unchanged when it fits`() {
        val text = Line(Span("hello", Style(ChromeRole.ACCENT)))

        assertEquals(text, text.ellipsize(5))
        assertEquals(text, text.ellipsize(8))
    }

    @Test
    fun `ellipsize cuts mid-span and styles the ellipsis with the interrupted span's style`() {
        val text = Line(Span("hello!", Style(ChromeRole.DANGER)))

        val truncated = text.ellipsize(5)

        assertEquals("hell…", truncated.plain)
        assertEquals(listOf(Span("hell…", Style(ChromeRole.DANGER))), truncated.spans)
    }

    @Test
    fun `ellipsize keeps a whole leading span intact when the cut falls in a later span`() {
        val text = Line(Span("Atlas", Style(ChromeRole.DANGER)), Span(" destroyed"))

        val truncated = text.ellipsize(7)

        assertEquals("Atlas …", truncated.plain)
        assertEquals(
            listOf(Span("Atlas", Style(ChromeRole.DANGER)), Span(" …", Style.DEFAULT)),
            truncated.spans,
        )
    }

    @Test
    fun `ellipsize of zero width is an empty line`() {
        val text = Line(Span("hello", Style(ChromeRole.ACCENT)))

        assertEquals(Line(emptyList()), text.ellipsize(0))
        assertEquals(0, text.ellipsize(0).width)
    }

    @Test
    fun `ellipsize of a negative width behaves as zero`() {
        val text = Line(Span("hello", Style(ChromeRole.ACCENT)))

        assertEquals(Line(emptyList()), text.ellipsize(-3))
    }

    @Test
    fun `an empty string yields an empty line`() {
        assertEquals(Line(emptyList()), Line("", Style(ChromeRole.ACCENT)))
    }

    @Test
    fun `plus concatenates two lines`() {
        val a = Line(Span("foo", Style(ChromeRole.ACCENT)))
        val b = Line(Span("bar", Style(ChromeRole.DANGER)))

        val sum = a + b

        assertEquals("foobar", sum.plain)
        assertEquals(
            listOf(Span("foo", Style(ChromeRole.ACCENT)), Span("bar", Style(ChromeRole.DANGER))),
            sum.spans,
        )
    }

    @Test
    fun `a cluster spanning spans takes the first base style`() {
        val text = Line(
            Span("e", Style(ChromeRole.DANGER)),
            Span("\u0301", Style(ChromeRole.SUCCESS)),
            Span("x", Style(ChromeRole.ACCENT)),
        )

        assertEquals("e\u0301x", text.plain)
        assertEquals(
            listOf(
                Span("e\u0301", Style(ChromeRole.DANGER)),
                Span("x", Style(ChromeRole.ACCENT)),
            ),
            text.spans,
        )
    }

    @Test
    fun `presentation clusters stay together`() {
        val text = Line(Span("👩", Style(ChromeRole.DANGER)), Span("‍💻", Style(ChromeRole.SUCCESS)))

        assertEquals(listOf(Span("👩‍💻", Style(ChromeRole.DANGER))), text.spans)
    }
}
