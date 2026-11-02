// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.color.StandardRole

/** A one-cell checkbox glyph for [state], in [color], with no global font or theme state. */
public class Checkbox(
    private val state: CheckState,
    private val color: ColorRole = intrinsicColor(state),
    private val glyphs: CheckboxGlyphs = CheckboxGlyphs.DEFAULT,
) : MeasurableWidget {

    override fun write(writer: RowWriter) {
        writer.write(0, checkboxIcon(state, glyphs), Style(color))
    }

    public companion object {
        /** Default per-state color when the surrounding row does not override it. */
        public fun intrinsicColor(state: CheckState): ColorRole = when (state) {
            CheckState.CHECKED -> StandardRole.SUCCESS
            else -> StandardRole.TEXT_MUTED
        }
    }
}
