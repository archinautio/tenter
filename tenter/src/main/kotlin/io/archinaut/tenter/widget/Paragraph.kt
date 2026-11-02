// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Style

/**
 * Lines of text. Without [wrap], each line takes one row and is ellipsized to the available width;
 * with it, lines wrap at word boundaries and a word wider than a row is split. An empty line is a
 * blank row.
 */
public class Paragraph(
    private val lines: List<Line>,
    private val wrap: Boolean = false,
) : MeasurableWidget {

    /** [text] split into lines at each `\n`, all in [style]. */
    public constructor(text: String, style: Style = Style.DEFAULT, wrap: Boolean = false) :
        this(text.split('\n').map { Line(it, style) }, wrap)

    override fun write(writer: RowWriter) {
        for (line in lines) {
            val rows = if (wrap) line.wrap(writer.width) else listOf(line)
            if (rows.isEmpty()) writer.newLine()
            rows.forEach(writer::writeLine)
        }
    }
}
