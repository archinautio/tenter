// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.shell

import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.widget.FlashMessage
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.Row
import io.archinaut.tenter.widget.RowWriter

/** One row: where the user is, or the page's [flash] while it shows one, and the [scheme] at the right. */
internal class StatusBar(
    private val location: String,
    private val flash: FlashMessage?,
    private val scheme: String,
) : MeasurableWidget {
    override fun write(writer: RowWriter) {
        val right = "scheme: $scheme"
        writer.draw(
            Row(
                listOf(
                    Constraint.Fill(1) to (flash ?: Paragraph(location, Style(StandardRole.TEXT_PRIMARY))),
                    Constraint.Length(CellWidth.of(right)) to Paragraph(right, Style(StandardRole.TEXT_MUTED)),
                ),
                spacing = 2,
            ),
        )
    }
}
