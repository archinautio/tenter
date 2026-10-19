// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.text.textClusters

/**
 * A rectangular, clipped, origin-translated region of a [Buffer]. All coordinates
 * passed to its methods are LOCAL: `(0, 0)` is this region's own top-left corner. Writes
 * outside `0 until width` / `0 until height` are silently dropped, so a widget rendering into a
 * [Canvas] cannot touch a sibling's cells — even by mistake.
 *
 * [inner] and [region] derive smaller canvases by [Rect.clipTo], so nesting can only
 * ever narrow: a child can't widen its way back out to a wider rect than it started with.
 */
public class Canvas private constructor(
    private val buffer: Buffer,
    /** This canvas's rectangle in [buffer]'s absolute coordinates. */
    private val bounds: Rect,
) {

    public val width: Int get() = bounds.width

    public val height: Int get() = bounds.height

    /** This canvas in its own local coordinates: always `Rect(0, 0, width, height)`. Split it with [Layout]. */
    public val area: Rect = Rect(0, 0, bounds.width, bounds.height)

    /** This canvas's top-left corner in its buffer's absolute (frame) coordinates. */
    internal val origin: Point get() = Point(bounds.x, bounds.y)

    /**
     * The intersection of [rect] with this canvas — e.g. a [Layout] split of [area], or a rectangle a
     * completed frame publishes (see [io.archinaut.tenter.widget.panel.PanelLayout]).
     */
    public fun region(rect: Rect): Canvas = Canvas(buffer, rect.clipTo(area).toAbsolute())

    /** The region left after removing [padding] from each edge, clamped to never go negative. */
    public fun inner(padding: Padding): Canvas = Canvas(buffer, area.inner(padding).toAbsolute())

    /** @throws IndexOutOfBoundsException if ([x], [y]) is outside this canvas. */
    public fun get(x: Int, y: Int): Cell {
        if (!area.contains(x, y)) {
            throw IndexOutOfBoundsException("($x, $y) out of bounds for ${width}x$height canvas")
        }
        return buffer.get(bounds.x + x, bounds.y + y)
    }

    /** No-op if ([x], [y]) is outside this canvas. */
    public fun set(x: Int, y: Int, cell: Cell) {
        if (!area.contains(x, y)) return
        buffer.writeCell(bounds.x + x, bounds.y + y, cell, bounds)
    }

    /** Writes [symbol] in [fg], preserving whatever background is already painted at ([x], [y]). */
    public fun setFg(x: Int, y: Int, symbol: String, fg: ColorRole) {
        if (!area.contains(x, y)) return
        val bg = buffer.get(bounds.x + x, bounds.y + y).style.bg
        buffer.writeCell(bounds.x + x, bounds.y + y, Cell(symbol, Style(fg, bg)), bounds)
    }

    public fun setString(
        x: Int,
        y: Int,
        text: String,
        style: Style = Style.DEFAULT,
    ) {
        var column = x
        for (cluster in textClusters(text)) {
            if (cluster.width == 0) continue
            buffer.writeCell(bounds.x + column, bounds.y + y, Cell(cluster.drawableText, style), bounds)
            column += cluster.width
        }
    }

    /**
     * Marks the rect content most wants visible — e.g. a selection cursor, a highlighted row.
     * Stored on the backing [Buffer] in absolute coords (translated through this canvas's
     * origin, like [set]), so it survives [region]/[inner] nesting and is readable from any
     * canvas over the same buffer via [revealRect]. A scrolling container (see `Viewport`) reads
     * it back to auto-follow. Clips like [set]: a rect (partly) outside this canvas is clamped,
     * and no-ops if left empty. At most one reveal rect exists per buffer — a later call
     * overwrites an earlier one.
     */
    public fun markReveal(rect: Rect) {
        val visible = rect.clipTo(area)
        if (visible.isEmpty) return
        buffer.reveal = visible.toAbsolute()
    }

    /** The rect last marked via [markReveal], translated into THIS canvas's local coords, or `null` if none. */
    public fun revealRect(): Rect? = buffer.reveal?.offset(-bounds.x, -bounds.y)

    /** Clears the reveal request in this canvas's backing frame. Used by prepared frame scopes. */
    internal fun clearReveal() {
        buffer.reveal = null
    }

    /**
     * Copies the whole of [src] to this canvas at ([destX], [destY]). To copy only part of a
     * canvas, narrow it first with [region]. Clipping is computed entirely in LOCAL coordinates
     * against both canvases' own bounds, then translated to the buffer once — so a blit can never
     * read or write past either canvas's edge, regardless of where each canvas sits in the shared
     * buffer.
     */
    public fun blit(src: Canvas, destX: Int = 0, destY: Int = 0) {
        val startCol = maxOf(0, -destX)
        val endCol = minOf(src.width, this.width - destX)
        val startRow = maxOf(0, -destY)
        val endRow = minOf(src.height, this.height - destY)
        if (startCol >= endCol || startRow >= endRow) return

        // Reading through a snapshot is what makes an overlapping blit within ONE buffer copy
        // original source data rather than cells this same loop already overwrote. Two distinct
        // buffers cannot overlap, and every prepared-content blit streams from its own offscreen
        // buffer — so the copy would otherwise cost a full duplicate of the content on every frame.
        val source = if (src.buffer === buffer) src.buffer.snapshotCells() else src.buffer.cellRows()
        val clip = Rect(
            bounds.x + destX + startCol,
            bounds.y + destY + startRow,
            (endCol - startCol),
            (endRow - startRow),
        )
        for (row in startRow until endRow) {
            var column = startCol
            while (column < endCol) {
                val sourceX = src.bounds.x + column
                val sourceY = src.bounds.y + row
                val stored = source[sourceY][sourceX]
                val completeWide = !stored.continuation && stored.width == 2 &&
                    column + 1 < endCol && sourceX + 1 < src.bounds.x + src.width &&
                    source[sourceY][sourceX + 1].continuation
                val destinationX = bounds.x + destX + column
                val destinationY = bounds.y + destY + row
                if (completeWide) {
                    buffer.writeCell(destinationX, destinationY, stored.cell, clip)
                    column += 2
                } else {
                    val cell = if (stored.continuation || stored.width == 2) {
                        Cell(" ", stored.cell.style)
                    } else {
                        stored.cell
                    }
                    buffer.writeCell(destinationX, destinationY, cell, clip)
                    column++
                }
            }
        }
    }

    private fun Rect.toAbsolute(): Rect = offset(bounds.x, bounds.y)

    public companion object {
        /** A canvas over the whole of [buffer]. */
        public fun of(buffer: Buffer): Canvas = Canvas(buffer, Rect(0, 0, buffer.width, buffer.height))

        /** A detached canvas over its own backing buffer — for measuring or scrolling content off-screen. */
        public fun offscreen(width: Int, height: Int): Canvas = of(Buffer(width, height))
    }
}
