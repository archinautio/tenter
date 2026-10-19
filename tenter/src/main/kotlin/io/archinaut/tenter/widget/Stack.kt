// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/**
 * Stacks [children] top to bottom, each at its measured height, with [gutter] blank rows between
 * them. Give a plain widget a size with [fixedSize] to stack it.
 */
@Suppress("FunctionName") // A factory named like the widget it builds, as Kotlin's List() is.
public fun Stack(children: List<MeasurableWidget>, gutter: Int = 1): MeasurableWidget {
    require(gutter >= 0) { "gutter must not be negative: $gutter" }
    return StackLayout(children.toList(), gutter)
}

private class StackLayout(
    private val children: List<MeasurableWidget>,
    private val gutter: Int,
) : LayoutWidget {

    override fun layout(availableWidth: Int): ContentLayout {
        require(availableWidth >= 0) { "available width must not be negative: $availableWidth" }
        val prepared = children.map { child ->
            child.prepare(availableWidth)
        }
        val width = prepared.maxOfOrNull { it.width } ?: 0
        val height = prepared.foldIndexed(0) { index, total, child ->
            val withChild = checkedAdd(total, child.height, "stack height")
            if (index < prepared.lastIndex) checkedAdd(withChild, gutter, "stack height") else withChild
        }
        return contentLayout(width, height) {
            var row = 0
            prepared.forEachIndexed { index, child ->
                place(0, row, child)
                row = checkedAdd(row, child.height, "stack placement")
                if (index < prepared.lastIndex) row = checkedAdd(row, gutter, "stack placement")
            }
        }
    }
}
