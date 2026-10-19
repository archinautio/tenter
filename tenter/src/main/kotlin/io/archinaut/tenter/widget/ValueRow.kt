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
) : MeasurableWidget {

    override fun write(cursor: TextCursor) {
        cursor.writeRow(left, right, Style(color))
        subLines.forEach { cursor.writeLine("    $it", Style(color)) }
    }
}
