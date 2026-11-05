// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import com.github.ajalt.mordant.rendering.TextAlign
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.screen.offset
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.text.TextTruncation

/** A run of titles placed on a block's row, starting at [column]. */
internal class PlacedTitles(val column: Int, val run: Line)

/**
 * Places [titles] on one row of a [width]-cell block, [CORNER_GAP] cells clear of each edge.
 *
 * Earlier titles come first: each takes what it needs of the room the ones before it left, its
 * text ellipsized to fit, and a title without room for its padding and a first glyph is dropped.
 * Titles of one alignment sit side by side in declaration order, separated only by their padding;
 * the centered run moves aside rather than cover the left or right one.
 */
internal fun placeTitles(titles: List<Title>, width: Int, textStyle: Style, paddingStyle: Style): List<PlacedTitles> {
    val room = width - 2 * CORNER_GAP
    var left = room
    val fitted = titles.filterNot { it.isEmpty }.mapNotNull { title ->
        fitted(title, left, textStyle, paddingStyle)?.let { run ->
            left -= run.width
            title.align.side() to run
        }
    }
    val runs = fitted.groupBy({ it.first }, { it.second }).mapValues { (_, lines) -> lines.reduce(Line::plus) }
    val start = runs[Side.LEFT]
    val end = runs[Side.RIGHT]
    val center = runs[Side.CENTER]
    val endColumn = width - CORNER_GAP - (end?.width ?: 0)
    return listOfNotNull(
        start?.let { PlacedTitles(CORNER_GAP, it) },
        center?.let {
            val centered = CORNER_GAP + TextAlign.CENTER.offset(it.width, room)
            PlacedTitles(centered.coerceIn(CORNER_GAP + (start?.width ?: 0), endColumn - it.width), it)
        },
        end?.let { PlacedTitles(endColumn, it) },
    )
}

/** [title] padded and styled within [room] cells, or null when not even its first glyph fits. */
private fun fitted(title: Title, room: Int, textStyle: Style, paddingStyle: Style): Line? {
    val padding = title.padding
    val available = room - padding.left - padding.right
    val text = Line(title.line.spans.map { Span(it.text, textStyle.patch(it.style)) })
    val shown = if (text.width <= available) text else text.ellipsize(available).takeIf { it.width > ELLIPSIS_WIDTH }
    return shown?.let { Line(" ".repeat(padding.left), paddingStyle) + it + Line(" ".repeat(padding.right), paddingStyle) }
}

private enum class Side { LEFT, CENTER, RIGHT }

private fun TextAlign.side(): Side = when (this) {
    TextAlign.CENTER -> Side.CENTER
    TextAlign.RIGHT -> Side.RIGHT
    TextAlign.LEFT, TextAlign.NONE, TextAlign.JUSTIFY -> Side.LEFT
}

/** Cells a title keeps clear of each edge of its row: the corner and one border cell. */
private const val CORNER_GAP: Int = 2

private val ELLIPSIS_WIDTH: Int = CellWidth.of(TextTruncation.ELLIPSIS)
