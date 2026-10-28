// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/** One column of [Columns]: [content] laid out at its declared [width] in cells. */
public data class Column(
    public val width: Int,
    public val content: MeasurableWidget,
) {
    init {
        require(width >= 0) { "column width must not be negative: $width" }
    }
}

/**
 * Packs [children] left-to-right, wrapping to a new band when the next one does not fit the
 * available width. Each [Column.width] is the declared column width; band height comes from the
 * children's measured heights. A child wider than the available space retains that declared width
 * and is clipped only when painted. Give a plain widget a size with [fixedSize] to place it in a
 * column. [gutter] separates columns within a band; wrapped bands are separated by one blank row.
 */
@Suppress("FunctionName") // A factory named like the widget it builds, as Kotlin's List() is.
public fun Columns(children: List<Column>, gutter: Int = 2): MeasurableWidget {
    require(gutter >= 0) { "gutter must not be negative: $gutter" }
    return ColumnsLayout(children.toList(), gutter)
}

private class ColumnsLayout(
    private val children: List<Column>,
    private val gutter: Int,
) : LayoutWidget {

    override fun layout(availableWidth: Int): ContentLayout {
        val prepared = children.map { child ->
            child to child.content.prepare(child.width)
        }

        var x = 0
        var bandTop = 0
        var bandHeight = 0
        var bandWidth = 0
        var totalWidth = 0
        val placements = mutableListOf<Placement>()

        for ((child, layout) in prepared) {
            val gutterBefore = if (x == 0) 0 else gutter
            if (x != 0 && x.toLong() + gutterBefore + child.width > availableWidth.toLong()) {
                bandTop = checkedAdd(
                    checkedAdd(bandTop, bandHeight, "columns height"),
                    1,
                    "columns height",
                )
                totalWidth = maxOf(totalWidth, bandWidth)
                x = 0
                bandHeight = 0
                bandWidth = 0
            }
            val childX = if (x == 0) 0 else checkedAdd(x, gutter, "columns placement")
            placements += Placement(childX, bandTop, layout)
            x = checkedAdd(childX, child.width, "columns placement")
            bandWidth = maxOf(bandWidth, x)
            bandHeight = maxOf(bandHeight, layout.height)
        }
        if (prepared.isNotEmpty()) {
            totalWidth = maxOf(totalWidth, bandWidth)
            bandTop = checkedAdd(bandTop, bandHeight, "columns height")
        }

        return contentLayout(totalWidth, bandTop) {
            placements.forEach { place(it.x, it.y, it.layout) }
        }
    }

    private data class Placement(val x: Int, val y: Int, val layout: ContentLayout)
}
