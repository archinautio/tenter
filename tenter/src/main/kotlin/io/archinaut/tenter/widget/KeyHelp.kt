// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.text.TextWrap

/**
 * Content for a HELP panel: a single `KEYS` section (a [Block] with a titled top border)
 * holding one sub-section per entry in [sections] — each rendered
 * lazygit-style as an indented label followed by its `key  description` rows.
 */
public class KeyHelp(private val sections: List<KeySection>) : MeasurableWidget {

    override fun write(cursor: TextCursor) {
        cursor.draw(MeasurableWidget { writeSections(it) }.block(HEADING))
    }

    private fun writeSections(cursor: TextCursor) {
        // One key column shared across every section (not per-hint) so descriptions line up in a
        // single straight edge down the panel instead of jagging with each key's own length.
        // The description is wrapped on its own — not as part of one combined "key + description"
        // string — because TextWrap collapses any run of whitespace to a single space, which would
        // destroy alignment padding embedded in the text.
        val keyColumnWidth = sections.flatMap { it.hints }.maxOfOrNull { CellWidth.of(it.keys) } ?: 0
        val prefixWidth = keyColumnWidth + 2
        val descWidth = (cursor.width - prefixWidth).coerceAtLeast(1)

        for ((index, section) in sections.withIndex()) {
            cursor.writeLine(section.title, SUBSECTION_STYLE)
            for (hint in section.hints) {
                TextWrap.wrap(hint.description, descWidth).forEachIndexed { i, wrapped ->
                    if (i == 0) cursor.write(0, hint.keys, KEY_STYLE)
                    cursor.write(prefixWidth, wrapped, DESC_STYLE)
                    cursor.newLine()
                }
            }
            if (index < sections.size - 1) cursor.newLine()
        }
    }

    private companion object {
        private val HEADING = Block(
            title = "KEYS",
            borders = Borders.TOP,
            borderColor = ChromeRole.INFO,
            titleColor = ChromeRole.INFO,
        )
        private val SUBSECTION_STYLE = Style(ChromeRole.ACCENT)
        private val KEY_STYLE = Style(ChromeRole.INFO)
        private val DESC_STYLE = Style(ChromeRole.TEXT_PRIMARY)
    }
}
