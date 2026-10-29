// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Constraint

/**
 * Lays [children] out left to right at their declared widths, wrapping to a new band when the
 * next one does not fit the available width, as text flows. Each band is a [Row] with [spacing]
 * blank cells between its children, and bands are stacked with one blank row between them. A
 * child wider than the available space keeps its declared width, alone in its band, and an
 * enclosing [Viewport] scrolls to it. Give a plain widget a size with [fixedSize] to flow it.
 */
@Suppress("FunctionName") // A factory named like the widget it builds, as Kotlin's List() is.
public fun Flow(children: List<Pair<Constraint.Length, MeasurableWidget>>, spacing: Int = 2): MeasurableWidget {
    require(spacing >= 0) { "spacing must not be negative: $spacing" }
    return FlowLayout(children.toList(), spacing)
}

private class FlowLayout(
    private val children: List<Pair<Constraint.Length, MeasurableWidget>>,
    private val spacing: Int,
) : LayoutWidget {

    override fun layout(availableWidth: Int): ContentLayout {
        val rows = bands(availableWidth).map { band ->
            AtLeast(bandWidth(band), Row(band, spacing))
        }
        return Stack(rows).prepare(availableWidth)
    }

    /** [children] grouped greedily into bands that fit [availableWidth]; each band holds at least one child. */
    private fun bands(availableWidth: Int): List<List<Pair<Constraint.Length, MeasurableWidget>>> {
        val bands = mutableListOf<MutableList<Pair<Constraint.Length, MeasurableWidget>>>()
        for (child in children) {
            val band = bands.lastOrNull()
            if (band != null && bandWidth(band) + spacing.toLong() + child.first.length <= availableWidth) {
                band += child
            } else {
                bands += mutableListOf(child)
            }
        }
        return bands
    }

    private fun bandWidth(band: List<Pair<Constraint.Length, MeasurableWidget>>): Int =
        band.fold(-spacing) { total, (length, _) -> checkedAdd(checkedAdd(total, spacing, FLOW_WIDTH), length.length, FLOW_WIDTH) }
}

/** [content] laid out at no less than [width], so a band keeps every declared width even when the space is narrower. */
private class AtLeast(private val width: Int, private val content: MeasurableWidget) : LayoutWidget {
    override fun layout(availableWidth: Int): ContentLayout = content.prepare(maxOf(availableWidth, width))
}

private const val FLOW_WIDTH: String = "flow width"
