// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.text.TextTruncation
import io.archinaut.tenter.text.TextWrap
import io.archinaut.tenter.text.textClusters

/** A run of text in one [style]. */
public data class Span(
    public val text: String,
    public val style: Style = Style.DEFAULT,
)

/**
 * One line of text whose styling varies along its length: a sequence of [Span]s that measures,
 * wraps, truncates, and paints as one unit, so a caller wanting two colors on one line never
 * computes a column.
 *
 * ```
 * Line("plain")
 * Line("warning", Style(StandardRole.WARNING))
 * Line(Span("key ", Style(StandardRole.INFO)), Span("description"))
 * ```
 *
 * Spans are canonical: empty spans are dropped and adjacent same-styled spans merged, so two lines
 * that paint identically are equal — which is what makes them usable in assertions.
 */
public class Line(spans: List<Span>) {

    public constructor(vararg spans: Span) : this(spans.asList())

    public constructor(text: String, style: Style = Style.DEFAULT) : this(listOf(Span(text, style)))

    public val spans: List<Span> = normalize(spans)

    /** Display width in terminal cells. */
    public val width: Int get() = CellWidth.of(plain)

    /** The unstyled projection. Code-unit indices used internally refer to it. */
    internal val plain: String by lazy(LazyThreadSafetyMode.NONE) { spans.joinToString("") { it.text } }

    public operator fun plus(other: Line): Line = Line(spans + other.spans)

    /** Span-aware [TextWrap.wrap]: identical break decisions, each row's styling sliced to match. */
    internal fun wrap(firstWidth: Int, continuationWidth: Int = firstWidth): List<Line> =
        TextWrap.wrapBy(plain, firstWidth, continuationWidth) { ranges ->
            val parts = mutableListOf<Span>()
            ranges.forEachIndexed { i, range ->
                // The separating space is the source's own, so it keeps the source's style —
                // which matters the moment a span carries a background.
                if (i > 0) parts += slice(range.first - 1, range.first)
                parts += slice(range.first, range.last + 1)
            }
            Line(parts)
        }

    /** Span-aware [TextTruncation.ellipsize]; the ellipsis inherits the style it interrupts. */
    internal fun ellipsize(maxWidth: Int): Line {
        val available = maxWidth.coerceAtLeast(0)
        if (width <= available) return this
        if (available == 0) return Line(emptyList())
        val keep = TextTruncation.prefixLengthWithin(plain, available - CellWidth.of(TextTruncation.ELLIPSIS))
        return Line(slice(0, keep) + Span(TextTruncation.ELLIPSIS, styleAt(spans, keep)))
    }

    /** The `[startIndex, endIndex)` slice of [plain], carrying its styling. */
    private fun slice(startIndex: Int, endIndex: Int): List<Span> = buildList {
        var offset = 0
        for (span in spans) {
            val spanEnd = offset + span.text.length
            val from = maxOf(startIndex, offset)
            val to = minOf(endIndex, spanEnd)
            if (from < to) add(Span(span.text.substring(from - offset, to - offset), span.style))
            offset = spanEnd
        }
    }

    override fun equals(other: Any?): Boolean = other is Line && spans == other.spans
    override fun hashCode(): Int = spans.hashCode()
    override fun toString(): String = "Line($spans)"

    private companion object {
        /** Drops empty spans and merges same-styled neighbours, styling each grapheme by its base. */
        fun normalize(spans: List<Span>): List<Span> {
            if (spans.all { it.text.isEmpty() }) return emptyList()
            val plain = spans.joinToString("") { it.text }
            val normalized = mutableListOf<Span>()
            for (cluster in textClusters(plain)) {
                val index = cluster.firstSpacingBaseIndex ?: cluster.startIndex
                val style = styleAt(spans, index)
                val last = normalized.lastOrNull()
                if (last != null && last.style == style) {
                    normalized[normalized.lastIndex] = last.copy(text = last.text + cluster.sourceText)
                } else {
                    normalized += Span(cluster.sourceText, style)
                }
            }
            return normalized
        }

        /** The style covering code-unit [index]; clamps to the last span so an ellipsis at the end works. */
        fun styleAt(spans: List<Span>, index: Int): Style {
            var offset = 0
            for (span in spans) {
                offset += span.text.length
                if (index < offset) return span.style
            }
            return spans.lastOrNull()?.style ?: Style.DEFAULT
        }
    }
}
