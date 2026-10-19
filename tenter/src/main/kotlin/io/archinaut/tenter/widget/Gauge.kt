// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.CellWidth

/**
 * A proportional `[███░░░]suffix` bar for [value], with the value right-aligned under the last
 * filled cell on the row below — two rows in all. [colorFor] selects the styling for [value].
 */
public class Gauge(
    private val value: Int,
    private val maxValue: Int,
    private val barWidth: Int,
    private val suffix: String = maxValue.toString(),
    private val colorFor: (value: Int) -> ColorRole = { value -> defaultColor(value, maxValue) },
) : ContentWidget {
    init {
        require(barWidth >= 0) { "barWidth must not be negative, was $barWidth" }
        require(maxValue >= 0) { "maxValue must not be negative, was $maxValue" }
    }

    override fun layout(availableWidth: Int): ContentLayout = contentWidget { content ->
        // maxValue <= 0 guard prevents division by zero. When maxValue == 0 the bar is all empty.
        // The bar is proportionally scaled: a fixed barWidth-cell bar spans the whole 0–maxValue
        // range (each block ≈ maxValue/barWidth units). The max sits inline after "]".
        val filled = if (maxValue <= 0) 0 else {
            (value.toLong() * barWidth / maxValue).coerceIn(0L, barWidth.toLong()).toInt()
        }
        val bar = "█".repeat(filled) + "░".repeat(barWidth - filled)
        val style = Style(colorFor(value))
        content.write(0, "[$bar]$suffix", style)
        content.newLine()
        val valueStr = value.toString()
        // First bar cell is at column 1 (the "[" prefix). Anchor on the last filled cell,
        // or the first cell when empty, then right-align the number to it.
        val anchorCol = filled.coerceAtLeast(1)
        content.write(anchorCol - CellWidth.of(valueStr) + 1, valueStr, style)
        content.newLine()
    }.layout(availableWidth)

    private companion object {
        fun defaultColor(value: Int, maxValue: Int): ChromeRole = when {
            value.toDouble() >= maxValue * 0.7 -> ChromeRole.DANGER
            value.toDouble() >= maxValue * 0.3 -> ChromeRole.WARNING
            else -> ChromeRole.INFO
        }
    }
}
