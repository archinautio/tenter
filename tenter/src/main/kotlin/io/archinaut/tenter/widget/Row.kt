// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Layout
import io.archinaut.tenter.screen.Rect

/**
 * Places [children] side by side across the available width, each at the width its [Constraint]
 * resolves to as [Layout.horizontal] splits a rectangle, with [spacing] blank cells between them.
 * Unlike [Flow], it never wraps.
 *
 * The row is as tall as its tallest child, and every child gets that full height, so a [block]
 * around a child draws its borders to the bottom of the row: `detail.block(Block(borders = Borders.LEFT))`
 * divides it from its neighbour. Like every composite, a row never clips: a child measured wider
 * than its width, such as fixed-size content, widens its segment and pushes the later ones right,
 * so an enclosing [Viewport] can scroll to it.
 */
@Suppress("FunctionName") // A factory named like the widget it builds, as Kotlin's List() is.
public fun Row(children: List<Pair<Constraint, MeasurableWidget>>, spacing: Int = 0): MeasurableWidget {
    require(spacing >= 0) { "spacing must not be negative: $spacing" }
    return RowLayout(children.toList(), spacing)
}

private class RowLayout(
    private val children: List<Pair<Constraint, MeasurableWidget>>,
    private val spacing: Int,
) : LayoutWidget {

    override fun layout(availableWidth: Int): ContentLayout {
        val segments = Layout.horizontal(children.map { it.first })
            .spacing(spacing)
            .split(Rect(0, 0, availableWidth, 0))
        val prepared = children.mapIndexed { index, (_, child) -> child.prepare(segments[index].width) }
        val height = prepared.maxOfOrNull { it.height } ?: 0

        var pushed = 0
        val placements = prepared.mapIndexed { index, child ->
            val segment = segments[index]
            val x = checkedAdd(segment.x, pushed, ROW_WIDTH)
            val width = maxOf(segment.width, child.width)
            pushed = checkedAdd(pushed, width - segment.width, ROW_WIDTH)
            Placement(x, child.stretchedTo(width, height))
        }
        val width = maxOf(availableWidth, placements.maxOfOrNull { checkedAdd(it.x, it.layout.width, ROW_WIDTH) } ?: 0)
        return contentLayout(width, height) {
            placements.forEach { place(it.x, 0, it.layout) }
        }
    }

    private data class Placement(val x: Int, val layout: ContentLayout)
}

private const val ROW_WIDTH: String = "row width"
