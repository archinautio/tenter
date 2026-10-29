// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.text.textClusters

/**
 * A clipped region of a [Buffer] in local coordinates, `(0, 0)` being its top-left corner. Writes
 * outside it are dropped, and the canvases derived from it can only be narrower.
 */
public class Canvas private constructor(
    private val buffer: Buffer,
    /** This canvas's logical rectangle — its origin and size — in [buffer]'s absolute coordinates. */
    private val bounds: Rect,
    /**
     * The absolute cells this canvas may write: [bounds] narrowed by every ancestor. Equal to
     * [bounds] except for a [window], whose logical rectangle may extend past its parent.
     */
    private val clip: Rect = bounds,
) {

    public val width: Int get() = bounds.width

    public val height: Int get() = bounds.height

    /** This canvas in its own coordinates: `Rect(0, 0, width, height)`. */
    public val area: Rect = Rect(0, 0, bounds.width, bounds.height)

    /** This canvas's top-left corner in its buffer's absolute (frame) coordinates. */
    internal val origin: Point get() = Point(bounds.x, bounds.y)

    /** The part of [area] this canvas can actually write, in local coordinates. */
    internal val visibleArea: Rect get() = clip.offset(-bounds.x, -bounds.y)

    /** The part of this canvas inside [rect]. */
    public fun region(rect: Rect): Canvas = narrowed(rect.clipTo(area))

    /** This canvas without [padding] on each edge. */
    public fun inner(padding: Padding): Canvas = narrowed(area.inner(padding))

    /**
     * A canvas of [rect]'s size whose origin is at [rect], even partly outside this canvas, writing
     * only where the two overlap; unlike [region], it can paint content shifted by a scroll offset.
     */
    internal fun window(rect: Rect): Canvas {
        val absolute = rect.toAbsolute()
        return Canvas(buffer, absolute, absolute.clipTo(clip))
    }

    private fun narrowed(local: Rect): Canvas {
        val absolute = local.toAbsolute()
        return Canvas(buffer, absolute, absolute.clipTo(clip))
    }

    /** @throws IndexOutOfBoundsException if ([x], [y]) is outside this canvas. */
    public fun get(x: Int, y: Int): Cell {
        if (!area.contains(x, y)) {
            throw IndexOutOfBoundsException("($x, $y) out of bounds for ${width}x$height canvas")
        }
        if (!visibleArea.contains(x, y)) return Cell.EMPTY
        return buffer.get(bounds.x + x, bounds.y + y)
    }

    /** No-op if ([x], [y]) is outside this canvas. */
    public fun set(x: Int, y: Int, cell: Cell) {
        if (!area.contains(x, y)) return
        buffer.writeCell(bounds.x + x, bounds.y + y, cell, clip)
    }

    /** Writes [text] from ([x], [y]), skipping cells outside this canvas, and returns the point after it. */
    public fun setString(
        x: Int,
        y: Int,
        text: String,
        style: Style = Style.DEFAULT,
    ): Point {
        var column = x
        for (cluster in textClusters(text)) {
            if (cluster.width == 0) continue
            buffer.writeCell(bounds.x + column, bounds.y + y, Cell(cluster.drawableText, style), clip)
            column += cluster.width
        }
        return Point(column, y)
    }

    /** Resets every cell this canvas can write to a blank that sets no style channel. */
    internal fun clear() {
        val visible = visibleArea
        for (y in visible.y until visible.y + visible.height) {
            for (x in visible.x until visible.x + visible.width) {
                buffer.writeCell(bounds.x + x, bounds.y + y, Cell.EMPTY, clip, replace = true)
            }
        }
    }

    private fun Rect.toAbsolute(): Rect = offset(bounds.x, bounds.y)

    public companion object {
        /** A canvas over the whole of [buffer]. */
        internal fun of(buffer: Buffer): Canvas = Canvas(buffer, Rect(0, 0, buffer.width, buffer.height))

        /** A detached canvas over its own buffer, for rendering a widget off-screen. */
        public fun offscreen(width: Int, height: Int): Canvas = of(Buffer(width, height))
    }
}
