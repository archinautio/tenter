// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.text.TextTruncation
import io.archinaut.tenter.text.TextWrap
import io.archinaut.tenter.text.textClusters

/**
 * One line of text made of styled [Span]s, measured, wrapped, truncated, and painted as a unit.
 * Empty spans are dropped and same-styled neighbours merged, so lines that paint alike are equal.
 */
public class Line(spans: List<Span>) {

    public constructor(vararg spans: Span) : this(spans.asList())

    public constructor(text: String, style: Style = Style.DEFAULT) : this(listOf(Span(text, style)))

    public val spans: List<Span> = normalize(spans)

    /** Display width in terminal cells. */
    public val width: Int by lazy(LazyThreadSafetyMode.NONE) { CellWidth.of(plain) }

    /** The unstyled projection. Code-unit indices used internally refer to it. */
    internal val plain: String by lazy(LazyThreadSafetyMode.NONE) { plainText(this.spans) }

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
        val keep = TextTruncation.prefixLengthWithin(plain, available - TextTruncation.ELLIPSIS_WIDTH)
        return Line(slice(0, keep) + Span(TextTruncation.ELLIPSIS, styleAt(spans, keep)))
    }

    /**
     * This line widened to [width] by spreading the missing cells over the space runs between its
     * words, earlier runs taking the remainder; each run's added spaces keep its last space's style.
     * A line without such a run, or already as wide, is returned unchanged.
     */
    internal fun justify(width: Int): Line {
        val missing = width - this.width
        if (missing <= 0) return this
        val clusters = textClusters(plain).toList()
        val firstWord = clusters.indexOfFirst { it.sourceText != " " }
        val lastWord = clusters.indexOfLast { it.sourceText != " " }
        if (firstWord < 0) return this
        // The code-unit index each interior run ends at, which is where its widening goes.
        val runEnds = (firstWord + 1..lastWord)
            .filter { clusters[it].sourceText != " " && clusters[it - 1].sourceText == " " }
            .map { clusters[it].startIndex }
        if (runEnds.isEmpty()) return this
        val each = missing / runEnds.size
        val remainder = missing % runEnds.size
        val widening = runEnds.withIndex().associate { (i, end) -> end to each + if (i < remainder) 1 else 0 }

        var offset = 0
        return Line(
            spans.map { span ->
                val text = buildString {
                    span.text.forEachIndexed { i, char ->
                        append(char)
                        widening[offset + i + 1]?.let { append(" ".repeat(it)) }
                    }
                }
                offset += span.text.length
                span.copy(text = text)
            },
        )
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
            val texts = spans.filter { it.text.isNotEmpty() }
            // One span styles every grapheme alike, so it is already normal.
            if (texts.size <= 1) return texts
            val plain = plainText(texts)
            val normalized = mutableListOf<Span>()
            var runStart = 0
            var runStyle: Style? = null
            for (cluster in textClusters(plain)) {
                val style = styleAt(texts, cluster.firstSpacingBaseIndex ?: cluster.startIndex)
                if (style == runStyle) continue
                if (runStyle != null) normalized += Span(plain.substring(runStart, cluster.startIndex), runStyle)
                runStart = cluster.startIndex
                runStyle = style
            }
            normalized += Span(plain.substring(runStart), checkNotNull(runStyle))
            return normalized
        }

        fun plainText(spans: List<Span>): String = spans.singleOrNull()?.text ?: spans.joinToString("") { it.text }

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
