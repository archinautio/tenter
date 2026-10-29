// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.textClusters

/** [text], one grapheme per row, centered horizontally, as a minimized panel's content. */
public class VerticalText(
    private val text: String,
    private val style: Style = Style(StandardRole.TEXT_PRIMARY),
) : MeasurableWidget {

    override fun write(writer: RowWriter) {
        for (cluster in textClusters(text)) {
            if (cluster.width == 0) continue
            writer.write(writer.width / 2, cluster.drawableText, style)
            writer.newLine()
        }
    }
}
