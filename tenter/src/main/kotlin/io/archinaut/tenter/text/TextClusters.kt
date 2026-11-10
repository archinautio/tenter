// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.text

import java.util.regex.Matcher
import java.util.regex.Pattern

/** One complete JDK-grapheme cluster and the metrics shared by text consumers. */
internal data class TextCluster(
    internal val startIndex: Int,
    internal val endIndex: Int,
    internal val sourceText: String,
    internal val drawableText: String,
    internal val width: Int,
    internal val firstSpacingBaseIndex: Int?,
)

private val GRAPHEME_PATTERN: Pattern = Pattern.compile("\\X")

/**
 * Splits [text] at JDK grapheme boundaries. The source range is retained for callers that must
 * slice the original string, while [TextCluster.drawableText] is sanitized for terminal paint.
 */
internal fun textClusters(text: String): Sequence<TextCluster> = Sequence {
    val scanner = ClusterScanner(text)
    object : Iterator<TextCluster> {
        override fun hasNext(): Boolean = scanner.hasNext()

        override fun next(): TextCluster {
            if (!scanner.hasNext()) throw NoSuchElementException()
            val start = scanner.position
            val end = scanner.next()
            if (end - start == 1 && isPrintableAscii(text[start])) {
                val glyph = asciiGlyph(text[start])
                return TextCluster(start, end, glyph, glyph, width = 1, firstSpacingBaseIndex = start)
            }
            return cluster(text, start, end)
        }
    }
}

/**
 * Calls [action] with the drawable text and width of each cluster of [text] that takes cells, as
 * [textClusters] would give them, without building a [TextCluster] for printable ASCII.
 */
internal inline fun forEachGlyph(text: String, action: (glyph: String, width: Int) -> Unit) {
    val scanner = ClusterScanner(text)
    while (scanner.hasNext()) {
        val start = scanner.position
        val end = scanner.next()
        if (end - start == 1 && isPrintableAscii(text[start])) {
            action(asciiGlyph(text[start]), 1)
        } else {
            val cluster = cluster(text, start, end)
            if (cluster.width > 0) action(cluster.drawableText, cluster.width)
        }
    }
}

/** [text] as its one cluster, or null when it holds none or several. */
internal fun singleCluster(text: String): TextCluster? {
    if (text.isEmpty()) return null
    // One code unit is one code point and so one cluster, without asking the regex.
    if (text.length == 1) return cluster(text, 0, 1)
    val scanner = ClusterScanner(text)
    return if (scanner.next() == text.length) cluster(text, 0, text.length) else null
}

/** The display width of the `[start, end)` code-unit slice of [text], segmented as if cut out of it. */
internal fun clustersWidth(text: String, start: Int = 0, end: Int = text.length): Int {
    val scanner = ClusterScanner(text, start, end)
    var width = scanner.skipAscii()
    while (scanner.hasNext()) {
        val clusterStart = scanner.position
        width += clusterWidth(text, clusterStart, scanner.next())
        width += scanner.skipAscii()
    }
    return width
}

/** The one-character string for printable ASCII [char], shared instead of allocated per cluster. */
internal fun asciiGlyph(char: Char): String = ASCII_GLYPHS[char.code - FIRST_PRINTABLE_ASCII]

internal fun isPrintableAscii(char: Char): Boolean = char.code in FIRST_PRINTABLE_ASCII..LAST_PRINTABLE_ASCII

/**
 * Walks the grapheme boundaries of `[start, end)` in [text], as if that slice were cut out of it.
 * Printable ASCII followed by ASCII is always a cluster of its own, the common case, so it skips the
 * regex; the matcher is created only when a cluster may span more than one character.
 */
internal class ClusterScanner(
    private val text: String,
    start: Int = 0,
    private val end: Int = text.length,
) {
    internal var position: Int = start
        private set

    private var matcher: Matcher? = null

    internal fun hasNext(): Boolean = position < end

    /** Moves past the run of one-character ASCII clusters ahead, possibly empty, and returns its length. */
    internal fun skipAscii(): Int {
        val from = position
        var index = from
        while (index < end && isAsciiCluster(index)) index++
        position = index
        return index - from
    }

    /** Moves past the next cluster and returns its end. */
    internal fun next(): Int {
        val start = position
        position = if (isAsciiCluster(start)) {
            start + 1
        } else {
            val matcher = matcher ?: GRAPHEME_PATTERN.matcher(matchedText()).also { matcher = it }
            matcher.region(start, end)
            check(matcher.lookingAt()) { "\\X matches at every index" }
            matcher.end()
        }
        return position
    }

    /** Whether printable ASCII at [index] is followed by ASCII or [end], so it is a cluster of its own. */
    private fun isAsciiCluster(index: Int): Boolean {
        val next = index + 1
        return isPrintableAscii(text[index]) && (next == end || text[next].code < ASCII_LIMIT)
    }

    /**
     * [text], cut at [end] when [end] splits a surrogate pair: the JDK decodes a pair whole even
     * across a region bound, but within the slice its high half is lone. Indices are unchanged.
     */
    private fun matchedText(): String =
        if (end < text.length && end > 0 && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) {
            text.substring(0, end)
        } else {
            text
        }
}

/** The cluster `[start, end)` of [text], measured and sanitized code point by code point. */
internal fun cluster(text: String, start: Int, end: Int): TextCluster {
    var index = start
    var width = 0
    var firstSpacingBaseIndex: Int? = null
    var sanitized = false
    val drawable = StringBuilder(end - start)
    while (index < end) {
        val codePoint = codePointWithin(text, index, end)
        val validCodePoint = if (codePoint in SURROGATE_RANGE) REPLACEMENT_CODE_POINT else codePoint
        if (isTerminalControl(validCodePoint)) {
            sanitized = true
        } else {
            if (validCodePoint != codePoint) sanitized = true
            drawable.appendCodePoint(validCodePoint)
            val codePointWidth = CellWidth.of(validCodePoint)
            width = maxOf(width, codePointWidth)
            // A spacing base is exactly a code point of positive width: marks, format and controls measure 0.
            if (firstSpacingBaseIndex == null && codePointWidth > 0) firstSpacingBaseIndex = index
        }
        index += Character.charCount(codePoint)
    }
    val source = text.substring(start, end)
    return TextCluster(
        startIndex = start,
        endIndex = end,
        sourceText = source,
        drawableText = if (sanitized) drawable.toString() else source,
        width = if (firstSpacingBaseIndex == null) 0 else width.coerceIn(0, 2),
        firstSpacingBaseIndex = firstSpacingBaseIndex,
    )
}

/** [TextCluster.firstSpacingBaseIndex] of the cluster `[start, end)` of [text], without building the cluster. */
internal fun firstSpacingBaseIndex(text: String, start: Int, end: Int): Int? {
    var index = start
    while (index < end) {
        val codePoint = codePointWithin(text, index, end)
        val validCodePoint = if (codePoint in SURROGATE_RANGE) REPLACEMENT_CODE_POINT else codePoint
        if (!isTerminalControl(validCodePoint) && CellWidth.of(validCodePoint) > 0) return index
        index += Character.charCount(codePoint)
    }
    return null
}

/** [cluster]'s width, without building the cluster. */
private fun clusterWidth(text: String, start: Int, end: Int): Int {
    var index = start
    var width = 0
    while (index < end) {
        val codePoint = codePointWithin(text, index, end)
        val validCodePoint = if (codePoint in SURROGATE_RANGE) REPLACEMENT_CODE_POINT else codePoint
        if (!isTerminalControl(validCodePoint)) width = maxOf(width, CellWidth.of(validCodePoint))
        index += Character.charCount(codePoint)
    }
    // No spacing base leaves every code point at 0, so the maximum is already [cluster]'s width.
    return width
}

/** The code point at [index] of [text], reading no further than [end], so a pair split by [end] is a lone surrogate. */
private fun codePointWithin(text: String, index: Int, end: Int): Int {
    val high = text[index]
    if (!high.isHighSurrogate() || index + 1 >= end) return high.code
    val low = text[index + 1]
    return if (low.isLowSurrogate()) Character.toCodePoint(high, low) else high.code
}

private fun isTerminalControl(codePoint: Int): Boolean = codePoint < 0x20 || codePoint in 0x7F..0x9F

private const val FIRST_PRINTABLE_ASCII: Int = 0x20
private const val LAST_PRINTABLE_ASCII: Int = 0x7E
private const val ASCII_LIMIT: Int = 0x80
private val ASCII_GLYPHS: Array<String> =
    Array(LAST_PRINTABLE_ASCII - FIRST_PRINTABLE_ASCII + 1) { (FIRST_PRINTABLE_ASCII + it).toChar().toString() }
private val SURROGATE_RANGE: IntRange = 0xD800..0xDFFF
private const val REPLACEMENT_CODE_POINT: Int = 0xFFFD
