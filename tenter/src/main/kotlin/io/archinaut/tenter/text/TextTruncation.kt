// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.text

internal object TextTruncation {

    const val ELLIPSIS: String = "…"

    val ELLIPSIS_WIDTH: Int = CellWidth.of(ELLIPSIS)

    fun ellipsize(text: String, maxWidth: Int): String {
        val availableWidth = maxWidth.coerceAtLeast(0)
        if (CellWidth.of(text) <= availableWidth) return text
        if (availableWidth == 0) return ""

        val keep = prefixLengthWithin(text, availableWidth - ELLIPSIS_WIDTH)
        return text.substring(0, keep) + ELLIPSIS
    }

    /** The end of the longest prefix of [text] that fits in [maxWidth] cells, never inside a grapheme cluster. */
    internal fun prefixLengthWithin(text: String, maxWidth: Int): Int {
        var displayWidth = 0
        var index = 0
        for (cluster in textClusters(text)) {
            if (displayWidth + cluster.width > maxWidth) break
            displayWidth += cluster.width
            index = cluster.endIndex
        }
        return index
    }
}
