// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.screen.Style

/**
 * One cursor-highlightable row: a selection marker, a checkbox, [label], and optionally a
 * right-aligned [right] value with [subLines] below it (see [ValueRow]). A [selected] row is
 * highlighted and requests visibility, so an enclosing [Viewport] follows it.
 * [checkboxColor] overrides the intrinsic checkbox color when the row is not selected.
 * [glyphs] selects the immutable checkbox glyph set without changing row colors.
 */
public class SelectableRow(
    private val label: String,
    private val checkState: CheckState,
    private val selected: Boolean,
    private val right: String? = null,
    private val subLines: List<String> = emptyList(),
    private val textColor: ColorRole = ChromeRole.TEXT_PRIMARY,
    private val checkboxColor: ColorRole? = null,
    private val glyphs: CheckboxGlyphs = CheckboxGlyphs.DEFAULT,
) : ContentWidget {

    override fun layout(availableWidth: Int): ContentLayout = contentWidget { content ->
        if (selected) content.markReveal()

        val color = if (selected) ChromeRole.ACCENT else textColor
        val marker = if (selected) "▶" else " "
        val left = "$marker   $label"
        if (right == null) {
            content.writeLine(left, Style(color))
        } else {
            content.draw(ValueRow(left, right, color, subLines))
        }

        val resolvedCheckboxColor = when {
            selected -> ChromeRole.ACCENT
            checkboxColor != null -> checkboxColor
            else -> Checkbox.intrinsicColor(checkState)
        }
        content.writeAt(CHECKBOX_COLUMN, 0, checkboxIcon(checkState, glyphs), Style(resolvedCheckboxColor))
    }.layout(availableWidth)

    private companion object {
        const val CHECKBOX_COLUMN = 2
    }
}
