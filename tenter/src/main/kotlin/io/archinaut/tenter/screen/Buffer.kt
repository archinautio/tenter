// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.text.singleCluster

internal class Buffer(
    val width: Int,
    val height: Int,
) {
    private val cells: Array<Array<StoredCell>>

    init {
        require(width >= 0) { "width must not be negative: $width" }
        require(height >= 0) { "height must not be negative: $height" }
        cells = Array(height) { Array(width) { EMPTY_STORED } }
    }

    // Declared after `init` so its size checks report before Rect's own.
    private val area: Rect = Rect(0, 0, width, height)

    /** Blanks every cell to [Cell.EMPTY], so the buffer can be redrawn from scratch. */
    fun reset() {
        for (row in cells) row.fill(EMPTY_STORED)
    }

    fun get(x: Int, y: Int): Cell {
        if (!area.contains(x, y)) {
            throw IndexOutOfBoundsException("($x, $y) out of bounds for ${width}x$height buffer")
        }
        return cells[y][x].asPublicCell()
    }

    fun set(x: Int, y: Int, symbol: String, style: Style = Style.DEFAULT) {
        if (!area.contains(x, y)) return
        write(x, y, symbol, style, area)
    }

    /**
     * Writes [symbol], one grapheme cluster, at ([x], [y]) within [clip], applying [style] to what
     * its lead column showed, so channels the style leaves unset — typically the background — keep
     * their painted value.
     */
    internal fun write(
        x: Int,
        y: Int,
        symbol: String,
        style: Style,
        clip: Rect,
    ) {
        val glyphSymbol: String
        val glyphWidth: Int
        // Every painted cell reaches here, and most are one printable ASCII character: one cluster,
        // one cell wide, so they skip grapheme segmentation and its allocations.
        if (symbol.isEmpty()) {
            glyphSymbol = " "
            glyphWidth = 1
        } else if (symbol.length == 1 && symbol[0].code in 0x20..0x7E) {
            glyphSymbol = symbol
            glyphWidth = 1
        } else {
            val cluster = singleCluster(symbol)
            require(cluster != null && cluster.width > 0) {
                "Cell must contain exactly one drawable grapheme cluster: ${symbol.toDebugString()}"
            }
            glyphSymbol = cluster.drawableText
            glyphWidth = cluster.width
        }
        writeGlyph(x, y, glyphSymbol, glyphWidth, style, clip)
    }

    /**
     * [write] for a [glyph] already known to be one sanitized, drawable grapheme cluster of
     * [glyphWidth] 1 or 2, as a segmenting caller has it, so it is not segmented again.
     */
    internal fun writeGlyph(
        x: Int,
        y: Int,
        glyph: String,
        glyphWidth: Int,
        style: Style,
        clip: Rect,
    ) {
        val clipLeft = clip.x
        val clipRight = clip.x + clip.width
        if (y !in clip.y until clip.y + clip.height) return
        if (glyphWidth == 2 && x < clipLeft && x + 1 >= clipLeft) {
            writeBlank(clipLeft, y, style, clip)
            return
        }
        if (x !in clipLeft until clipRight) return

        if (glyphWidth == 2 && x + 1 >= clipRight) {
            writeBlank(x, y, style, clip)
            return
        }

        val targetEnd = x + glyphWidth
        if (targetEnd > clipRight || targetEnd > width) return
        if (!canReplace(x, y, targetEnd, clip)) return

        val painted = cells[y][x].cell.painted(glyph, style)
        clearGlyphAt(x, y)
        if (glyphWidth == 2) clearGlyphAt(x + 1, y)
        cells[y][x] = StoredCell(painted, glyphWidth, continuation = false)
        if (glyphWidth == 2) {
            cells[y][x + 1] = StoredCell(painted, width = 2, continuation = true)
        }
    }

    /** Resets ([x], [y]) within [clip] to [Cell.EMPTY]; a wide glyph it would split is left whole. */
    internal fun reset(x: Int, y: Int, clip: Rect) {
        if (!canReplace(x, y, x + 1, clip)) return
        clearGlyphAt(x, y)
        cells[y][x] = EMPTY_STORED
    }

    internal fun snapshot(): Buffer {
        val copy = Buffer(width, height)
        for (y in 0 until height) {
            copy.cells[y] = cells[y].copyOf()
        }
        return copy
    }

    internal fun glyphStart(x: Int, y: Int): Int = if (cells[y][x].continuation) x - 1 else x

    internal fun glyphEndExclusive(x: Int, y: Int): Int {
        val cell = cells[y][x]
        return if (!cell.continuation && cell.width == 2) x + 2 else glyphStart(x, y) + 1
    }

    private fun canReplace(
        x: Int,
        y: Int,
        targetEnd: Int,
        clip: Rect,
    ): Boolean {
        if (y !in clip.y until clip.y + clip.height) return false
        val clipLeft = clip.x
        val clipRight = clip.x + clip.width
        // `targetEnd - x` is 1 or 2, and the two columns of a wide glyph share one lead, so this
        // walks at most two distinct leads — worth doing without allocating a list per cell write.
        var column = x
        var previousLead = -1
        while (column < targetEnd) {
            val lead = glyphStart(column, y)
            if (lead != previousLead) {
                val stored = cells[y][lead]
                if (lead < clipLeft || lead + stored.width > clipRight || lead + stored.width > width) return false
                previousLead = lead
            }
            column++
        }
        return true
    }

    /**
     * Blanks the wide glyph covering ([x], [y]), so overwriting one of its columns leaves no half
     * behind. Every caller then overwrites ([x], [y]) itself, so a narrow glyph there needs nothing.
     */
    private fun clearGlyphAt(x: Int, y: Int) {
        val lead = glyphStart(x, y)
        val stored = cells[y][lead]
        if (stored.width == 1) return
        val blank = StoredCell(stored.cell.copy(symbol = " "), width = 1, continuation = false)
        cells[y][lead] = blank
        if (stored.width == 2 && lead + 1 < width) {
            cells[y][lead + 1] = blank
        }
    }

    private fun writeBlank(
        x: Int,
        y: Int,
        style: Style,
        clip: Rect,
    ) {
        if (!canReplace(x, y, x + 1, clip)) return
        val painted = cells[y][x].cell.painted(" ", style)
        clearGlyphAt(x, y)
        cells[y][x] = StoredCell(painted, width = 1, continuation = false)
    }
}

private data class StoredCell(
    val cell: Cell,
    val width: Int,
    val continuation: Boolean,
) {
    fun asPublicCell(): Cell = if (continuation) cell.copy(symbol = "") else cell
}

private fun String.toDebugString(): String = replace("\u001B", "<ESC>")

private val EMPTY_STORED: StoredCell = StoredCell(Cell.EMPTY, width = 1, continuation = false)
