// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Layout
import io.archinaut.tenter.screen.Rect

/**
 * Places [children] side by side across the available width, each at the width its [Constraint]
 * resolves to as [Layout.horizontal] splits a rectangle, with [spacing] blank cells between them.
 * Unlike [Columns], it never wraps.
 *
 * The row is as tall as its tallest child, and every child gets that full height, so a [block]
 * around a child draws its borders to the bottom of the row: `detail.block(Block(borders = Borders.LEFT))`
 * divides it from its neighbour. A child measured wider than its width is clipped to it.
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
        return contentLayout(availableWidth, height) {
            prepared.forEachIndexed { index, child ->
                val segment = segments[index]
                place(segment.x, 0, child.fitTo(segment.width, height))
            }
        }
    }
}
