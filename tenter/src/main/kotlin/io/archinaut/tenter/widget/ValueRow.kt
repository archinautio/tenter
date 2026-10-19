// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.screen.Style

/** "<left> … <right>" on one row, then one indented line per entry in [subLines], all in [color]. */
public class ValueRow(
    private val left: String,
    private val right: String,
    private val color: ColorRole,
    private val subLines: List<String> = emptyList(),
) : ContentWidget {

    override fun layout(availableWidth: Int): ContentLayout = contentWidget { content ->
        content.writeRow(left, right, Style(color))
        subLines.forEach { content.writeLine("    $it", Style(color)) }
    }.layout(availableWidth)
}
