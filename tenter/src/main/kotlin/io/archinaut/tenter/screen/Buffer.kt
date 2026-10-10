// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.text.textClusters

public class Buffer(
    public val width: Int,
    public val height: Int,
) {
    private val cells: Array<Array<StoredCell>>

    init {
        require(width >= 0) { "width must not be negative: $width" }
        require(height >= 0) { "height must not be negative: $height" }
        cells = Array(height) { Array(width) { EMPTY_STORED } }
    }

    // Declared after `init` so its size checks report before Rect's own.
    private val area: Rect = Rect(0, 0, width, height)

    /** The reveal rect last marked via [Canvas.markReveal], in this buffer's own absolute coords. */
    internal var reveal: Rect? = null

    /** Blanks every cell to [Cell.EMPTY] and drops any reveal request, so the buffer can be redrawn from scratch. */
    public fun reset() {
        for (row in cells) row.fill(EMPTY_STORED)
        reveal = null
    }

    public fun get(x: Int, y: Int): Cell {
        if (!area.contains(x, y)) {
            throw IndexOutOfBoundsException("($x, $y) out of bounds for ${width}x$height buffer")
        }
        return cells[y][x].asPublicCell()
    }

    public fun set(x: Int, y: Int, cell: Cell) {
        if (!area.contains(x, y)) return
        writeCell(x, y, cell, area)
    }

    internal fun writeCell(
        x: Int,
        y: Int,
        cell: Cell,
        clip: Rect,
    ) {
        val glyph = cell.toStoredGlyph()
        val clipLeft = clip.x
        val clipRight = clip.x + clip.width
        if (y !in clip.y until clip.y + clip.height) return
        if (glyph.width == 2 && x < clipLeft && x + 1 >= clipLeft) {
            writeBlank(clipLeft, y, glyph.cell.style, clip)
            return
        }
        if (x !in clipLeft until clipRight) return

        if (glyph.width == 2 && x + 1 >= clipRight) {
            writeBlank(x, y, glyph.cell.style, clip)
            return
        }

        val targetEnd = x + glyph.width
        if (targetEnd > clipRight || targetEnd > width) return
        if (!canReplace(x, y, targetEnd, clip)) return

        clearGlyphAt(x, y)
        if (glyph.width == 2) clearGlyphAt(x + 1, y)
        cells[y][x] = glyph
        if (glyph.width == 2) {
            cells[y][x + 1] = StoredCell(glyph.cell, width = 2, continuation = true)
        }
    }

    internal fun snapshot(): Buffer {
        val copy = Buffer(width, height)
        for (y in 0 until height) {
            copy.cells[y] = cells[y].copyOf()
        }
        return copy
    }

    /** A defensive copy, for a reader that writes back into this same buffer while iterating. */
    internal fun snapshotCells(): Array<Array<StoredCell>> = Array(height) { y -> cells[y].copyOf() }

    /** Direct read access for a reader whose writes land in a different buffer. */
    internal fun cellRows(): Array<Array<StoredCell>> = cells

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

    private fun clearGlyphAt(x: Int, y: Int) {
        val lead = glyphStart(x, y)
        val stored = cells[y][lead]
        cells[y][lead] = StoredCell(Cell(" ", stored.cell.style), width = 1, continuation = false)
        if (stored.width == 2 && lead + 1 < width) {
            cells[y][lead + 1] = StoredCell(Cell(" ", stored.cell.style), width = 1, continuation = false)
        }
    }

    private fun writeBlank(
        x: Int,
        y: Int,
        style: Style,
        clip: Rect,
    ) {
        if (!canReplace(x, y, x + 1, clip)) return
        clearGlyphAt(x, y)
        cells[y][x] = StoredCell(Cell(" ", style), width = 1, continuation = false)
    }

    private fun Cell.toStoredGlyph(): StoredCell {
        if (symbol.isEmpty()) return StoredCell(Cell(" ", style), width = 1, continuation = false)
        // Every cell of every blit and every cluster of every setString reaches this method, and
        // full grapheme segmentation allocates a matcher, a sequence and a builder per call. A
        // lone printable ASCII character is unambiguously one cluster one cell wide, which is what
        // the vast majority of painted cells are.
        if (symbol.length == 1 && symbol[0].code in 0x20..0x7E) {
            return StoredCell(this, width = 1, continuation = false)
        }
        val clusters = textClusters(symbol).toList()
        require(clusters.size == 1 && clusters.single().width > 0) {
            "Cell must contain exactly one drawable grapheme cluster: ${symbol.toDebugString()}"
        }
        val cluster = clusters.single()
        return StoredCell(Cell(cluster.drawableText, style), cluster.width, continuation = false)
    }
}

internal data class StoredCell(
    internal val cell: Cell,
    internal val width: Int,
    internal val continuation: Boolean,
) {
    internal fun asPublicCell(): Cell = if (continuation) Cell("", cell.style) else cell
}

private fun String.toDebugString(): String = replace("\u001B", "<ESC>")

private val EMPTY_STORED: StoredCell = StoredCell(Cell.EMPTY, width = 1, continuation = false)
