// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.textClusters

/**
 * [text], one grapheme per row, centered horizontally in whatever width it is given — content
 * only, no border. Suits a minimized panel's stub, which [io.archinaut.tenter.widget.panel.Panels]
 * wraps in its own chrome like every other presentation.
 */
public class VerticalText(
    private val text: String,
    private val style: Style = Style(ChromeRole.TEXT_PRIMARY),
) : MeasurableWidget {

    override fun write(cursor: TextCursor) {
        for (cluster in textClusters(text)) {
            if (cluster.width == 0) continue
            cursor.write(cursor.width / 2, cluster.drawableText, style)
            cursor.newLine()
        }
    }
}
