// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph

/** The two kinds of text every page uses to explain itself. */
internal object PageSections {
    /** A section title naming what the content below it shows. */
    internal fun heading(text: String): MeasurableWidget = Paragraph(text, Style(StandardRole.INFO, bold = true))

    /** Explanatory prose, wrapped to the panel. */
    internal fun note(text: String): MeasurableWidget = Paragraph(text, Style(StandardRole.TEXT_MUTED), wrap = true)
}
