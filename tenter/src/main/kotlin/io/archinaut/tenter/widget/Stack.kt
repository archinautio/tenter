// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/**
 * Stacks [children] top to bottom, each at its measured height, with [spacing] blank rows between
 * them. Give a plain widget a size with [fixedSize] to stack it.
 */
@Suppress("FunctionName") // A factory named like the widget it builds, as Kotlin's List() is.
public fun Stack(children: List<MeasurableWidget>, spacing: Int = 1): MeasurableWidget {
    require(spacing >= 0) { "spacing must not be negative: $spacing" }
    val stacked = children.toList()
    return MeasurableWidget { writer ->
        stacked.forEachIndexed { index, child ->
            if (index > 0) repeat(spacing) { writer.newLine() }
            writer.draw(child)
        }
    }
}
