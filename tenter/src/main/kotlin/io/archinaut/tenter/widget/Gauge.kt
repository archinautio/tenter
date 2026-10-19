// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.CellWidth

/**
 * One row showing [ratio] of a whole, in the manner of Ratatui's `LineGauge`: an optional [label],
 * then a bar across the rest of the width whose filled part is [filledStyle] and remainder
 * [unfilledStyle]. The caller chooses the styles, for example to color a nearly full gauge.
 */
public class Gauge(
    private val ratio: Double,
    private val label: String = "",
    private val filledStyle: Style = Style(ChromeRole.INFO),
    private val unfilledStyle: Style = Style(ChromeRole.TEXT_MUTED),
) : MeasurableWidget {

    init {
        require(ratio in 0.0..1.0) { "gauge ratio must be within 0..1, was $ratio" }
    }

    override fun write(cursor: TextCursor) {
        val barStart = if (label.isEmpty()) 0 else CellWidth.of(label) + 1
        val barWidth = (cursor.width - barStart).coerceAtLeast(0)
        val filled = (ratio * barWidth).toInt().coerceIn(0, barWidth)
        if (label.isNotEmpty()) cursor.write(0, label)
        cursor.write(barStart, "█".repeat(filled), filledStyle)
        cursor.write(barStart + filled, "░".repeat(barWidth - filled), unfilledStyle)
        cursor.newLine()
    }
}
