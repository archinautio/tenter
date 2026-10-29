// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.text.TextWrap

/** Content for a help panel: a titled `KEYS` section with an indented sub-section of `key  description` rows per entry in [sections]. */
public class KeyHelp(private val sections: List<KeySection>) : MeasurableWidget {

    override fun write(writer: RowWriter) {
        writer.draw(MeasurableWidget { writeSections(it) }.block(HEADING))
    }

    private fun writeSections(writer: RowWriter) {
        // One key column across every section, so descriptions line up. Descriptions wrap on their
        // own because wrapping collapses runs of spaces, which would undo the alignment.
        val keyColumnWidth = sections.flatMap { it.hints }.maxOfOrNull { CellWidth.of(it.keys) } ?: 0
        val prefixWidth = keyColumnWidth + 2
        val descWidth = (writer.width - prefixWidth).coerceAtLeast(1)

        for ((index, section) in sections.withIndex()) {
            writer.writeLine(section.title, SUBSECTION_STYLE)
            for (hint in section.hints) {
                TextWrap.wrap(hint.description, descWidth).forEachIndexed { i, wrapped ->
                    if (i == 0) writer.write(0, hint.keys, KEY_STYLE)
                    writer.write(prefixWidth, wrapped, DESC_STYLE)
                    writer.newLine()
                }
            }
            if (index < sections.size - 1) writer.newLine()
        }
    }

    private companion object {
        private val HEADING = Block(
            title = "KEYS",
            borders = Borders.TOP,
            borderColor = StandardRole.INFO,
            titleColor = StandardRole.INFO,
        )
        private val SUBSECTION_STYLE = Style(StandardRole.ACCENT)
        private val KEY_STYLE = Style(StandardRole.INFO)
        private val DESC_STYLE = Style(StandardRole.TEXT_PRIMARY)
    }
}
