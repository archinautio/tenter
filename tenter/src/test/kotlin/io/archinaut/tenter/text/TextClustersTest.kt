// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class TextClustersTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "plain ascii text",
            "é and ä", // ASCII bases taking combining marks
            "x‍y", // ASCII joined by ZWJ
            "a️ b⃣", // ASCII with a variation selector and an enclosing mark
            "1️⃣ #️⃣", // keycaps start with ASCII
            "line\r\nbreak\n\ttab\u001B[0m",
            "🇪🇸🇫🇷🇩 odd regional indicators",
            "👩‍💻 at 中文 ✅ and ❤️",
            "\uD800 lone \uDC00 surrogates",
            "́ leading mark",
            "\u0085next-line control",
        ],
    )
    fun `clusters match JDK grapheme segmentation`(text: String) {
        val expected = Regex("\\X").findAll(text).map { it.range.first to it.range.last + 1 }.toList()

        val clusters = textClusters(text).toList()

        assertEquals(expected, clusters.map { it.startIndex to it.endIndex })
        assertEquals(clusters.map { it.sourceText }, expected.map { (start, end) -> text.substring(start, end) })
        assertEquals(clusters.sumOf { it.width }, CellWidth.of(text))
    }

    @ParameterizedTest
    @ValueSource(strings = ["étude", "a‍b c", "ok 👩‍💻 go", "\r\nx"])
    fun `a slice is measured as if cut out of the text`(text: String) {
        for (start in 0..text.length) {
            for (end in start..text.length) {
                assertEquals(
                    textClusters(text.substring(start, end)).sumOf { it.width },
                    CellWidth.of(text, start, end),
                    "[$start, $end) of ${text.toList()}",
                )
            }
        }
    }

    @Test
    fun `drawable text drops controls and replaces lone surrogates`() {
        val clusters = textClusters("a\u0007\uD800").toList()

        assertEquals(listOf("a", "", "�"), clusters.map { it.drawableText })
        assertEquals(listOf(1, 0, 1), clusters.map { it.width })
    }

    @Test
    fun `an unsanitized cluster draws its source text`() {
        val cluster = textClusters("中").single()

        assertSame(cluster.sourceText, cluster.drawableText)
    }

    @Test
    fun `a spacing base after leading marks is located`() {
        val cluster = textClusters("‍a").toList()

        assertEquals(listOf(null, 1), cluster.map { it.firstSpacingBaseIndex })
    }
}
