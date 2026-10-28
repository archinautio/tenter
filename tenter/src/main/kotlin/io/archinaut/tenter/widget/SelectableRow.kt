// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.screen.Style

/**
 * One cursor-highlightable row: a selection marker, a checkbox, [label], and optionally a
 * right-aligned [right] value. A [selected] row is highlighted and requests visibility, so an
 * enclosing [Viewport] follows it.
 * [checkboxColor] overrides the intrinsic checkbox color when the row is not selected.
 * [glyphs] selects the immutable checkbox glyph set without changing row colors.
 */
public class SelectableRow(
    private val label: String,
    private val checkState: CheckState,
    private val selected: Boolean,
    private val right: String? = null,
    private val textColor: ColorRole = StandardRole.TEXT_PRIMARY,
    private val checkboxColor: ColorRole? = null,
    private val glyphs: CheckboxGlyphs = CheckboxGlyphs.DEFAULT,
) : MeasurableWidget {

    override fun write(writer: RowWriter) {
        val row = MeasurableWidget(::writeRow)
        writer.draw(if (selected) row.revealed() else row)
    }

    private fun writeRow(writer: RowWriter) {
        val color = if (selected) StandardRole.ACCENT else textColor
        val marker = if (selected) "▶" else " "
        val left = "$marker   $label"
        if (right == null) {
            writer.writeLine(left, Style(color))
        } else {
            writer.writeRow(left, right, Style(color))
        }

        val resolvedCheckboxColor = when {
            selected -> StandardRole.ACCENT
            checkboxColor != null -> checkboxColor
            else -> Checkbox.intrinsicColor(checkState)
        }
        writer.write(CHECKBOX_COLUMN, checkboxIcon(checkState, glyphs), Style(resolvedCheckboxColor), row = 0)
    }

    private companion object {
        const val CHECKBOX_COLUMN = 2
    }
}
