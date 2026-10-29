// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.animation

import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.widget.Widget

/**
 * A mutable grid of one-cell glyphs with priority compositing and per-glyph styles, for building
 * animation frames. In-bounds writes reject characters that are not exactly one cell wide;
 * out-of-bounds writes are dropped. [priority] and [style] must give stable results, and a
 * finished frame must not be mutated again.
 */
public class GlyphGrid(
    public val size: Size,
    private val priority: (Char) -> Int,
    private val style: (Char) -> Style,
) : Widget {
    init {
        require(size.width > 0 && size.height > 0) { "grid size must be positive, was $size" }
    }

    public val width: Int = size.width
    public val height: Int = size.height

    private val cells: Array<CharArray> = Array(height) { CharArray(width) { ' ' } }

    /** The glyph at ([x], [y]). Throws if the coordinates are outside this grid. */
    public fun get(x: Int, y: Int): Char = cells[y][x]

    /** The style supplied for the glyph at ([x], [y]). */
    public fun styleAt(x: Int, y: Int): Style = style(get(x, y))

    /** Writes [char] unless a higher-priority glyph occupies the cell. */
    public fun put(x: Int, y: Int, char: Char) {
        if (!inBounds(x, y)) return
        validate(char)
        if (priority(char) >= priority(cells[y][x])) cells[y][x] = char
    }

    /** Writes [char] regardless of the current cell priority. */
    public fun set(x: Int, y: Int, char: Char) {
        if (!inBounds(x, y)) return
        validate(char)
        cells[y][x] = char
    }

    /** Paints every cell, including styled spaces. */
    public override fun render(canvas: Canvas) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                canvas.set(x, y, Cell(get(x, y).toString(), styleAt(x, y)))
            }
        }
    }

    private fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    private fun validate(char: Char) {
        require(!char.isSurrogate()) { "animation glyph must not be a surrogate: U+%04X".format(char.code) }
        require(CellWidth.of(char.code) == 1) {
            "animation glyph must occupy one terminal cell: U+%04X".format(char.code)
        }
    }
}
