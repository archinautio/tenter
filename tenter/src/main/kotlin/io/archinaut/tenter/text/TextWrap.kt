// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.text

internal object TextWrap {

    /**
     * Each output row as the inclusive index ranges of its words in [text], so a caller carrying
     * per-index styling slices at the same breaks. Words on a row are joined by one space; a word
     * wider than the row is split between grapheme clusters.
     */
    fun <T> wrapBy(
        text: String,
        firstWidth: Int,
        continuationWidth: Int = firstWidth,
        row: (List<IntRange>) -> T,
    ): List<T> {
        val firstCap = firstWidth.coerceAtLeast(1)
        val contCap = continuationWidth.coerceAtLeast(1)

        val rows = mutableListOf<T>()
        var current = mutableListOf<IntRange>()
        var currentWidth = 0
        var capacity = firstCap

        fun flush() {
            if (current.isNotEmpty()) {
                rows += row(current)
                current = mutableListOf()
                currentWidth = 0
            }
        }

        forEachWord(text) { start, end ->
            val sepWidth = if (current.isEmpty()) 0 else 1
            val wordWidth = CellWidth.of(text, start, end)
            val needed = sepWidth + wordWidth
            if (currentWidth + needed <= capacity) {
                current += start until end
                currentWidth += needed
            } else {
                flush()
                capacity = contCap
                if (wordWidth <= capacity) {
                    current += start until end
                    currentWidth = wordWidth
                } else {
                    var chunkStart = start
                    var chunkWidth = 0
                    for (cluster in textClusters(text)) {
                        if (cluster.startIndex < start) continue
                        if (cluster.startIndex >= end) break
                        if (cluster.width > capacity) {
                            if (chunkStart < cluster.startIndex) rows += row(listOf(chunkStart until cluster.startIndex))
                            rows += row(listOf(cluster.startIndex until cluster.endIndex))
                            chunkStart = cluster.endIndex
                            chunkWidth = 0
                        } else {
                            if (chunkWidth > 0 && chunkWidth + cluster.width > capacity) {
                                rows += row(listOf(chunkStart until cluster.startIndex))
                                chunkStart = cluster.startIndex
                                chunkWidth = 0
                            }
                            chunkWidth += cluster.width
                        }
                    }
                    if (chunkStart < end) {
                        current += chunkStart until end
                        currentWidth = chunkWidth
                    }
                }
            }
        }
        flush()
        return rows
    }

    fun wrap(text: String, firstWidth: Int, continuationWidth: Int = firstWidth): List<String> =
        wrapBy(text, firstWidth, continuationWidth) { ranges ->
            ranges.joinToString(" ") { text.substring(it.first, it.last + 1) }
        }

    /** Maximal runs of non-space characters, as `[start, end)` pairs — what `split(' ')` tokenized. */
    private inline fun forEachWord(text: String, action: (start: Int, end: Int) -> Unit) {
        var i = 0
        while (i < text.length) {
            while (i < text.length && text[i] == ' ') i++
            if (i >= text.length) break
            val start = i
            while (i < text.length && text[i] != ' ') i++
            action(start, i)
        }
    }
}
