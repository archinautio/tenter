// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import io.archinaut.tenter.animation.Animation
import io.archinaut.tenter.animation.GlyphGrid
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.Widget
import kotlin.time.Duration

// --8<-- [start:animation]
/** [rows] of text moving from right to left across [width] cells, one cell per frame, wrapping around. */
internal class MarqueeAnimation(
    private val rows: List<String>,
    width: Int,
    override val frameDuration: Duration,
    private val style: Style,
) : Animation {
    override val size: Size = Size(width, rows.size)
    override val frameCount: Int = width

    init {
        require(rows.all { it.length <= width }) { "every row must fit in $width cells" }
    }

    override fun frame(index: Int): Widget {
        val grid = GlyphGrid(
            size = size,
            priority = { if (it == ' ') 0 else 1 },
            style = { if (it == ' ') Style.DEFAULT else style },
        )
        for ((y, row) in rows.withIndex()) {
            for ((x, glyph) in row.withIndex()) {
                grid.set((x - index).mod(size.width), y, glyph)
            }
        }
        return grid
    }
}
// --8<-- [end:animation]
