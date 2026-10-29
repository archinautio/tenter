// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.screen.Style

/**
 * A thumb showing where a [Viewport]'s settled frame sits along one axis, painted along the
 * canvas's first column or row; nothing while the content fits.
 */
public class Scrollbar private constructor(
    private val axis: Axis,
    private val style: Style,
) : StatefulWidget<ScrollState> {

    override fun render(canvas: Canvas, state: ScrollState) {
        val track = axis.track(canvas)
        thumb(track, axis.length(state.content), axis.length(state.viewport), axis.offset(state))
            ?.forEach { axis.paint(canvas, it, Cell(axis.glyph, style)) }
    }

    private sealed class Axis(val glyph: String) {
        abstract fun track(canvas: Canvas): Int

        abstract fun length(size: Size): Int

        abstract fun offset(state: ScrollState): Int

        abstract fun paint(canvas: Canvas, position: Int, cell: Cell)

        object Vertical : Axis("▐") {
            override fun track(canvas: Canvas): Int = canvas.height

            override fun length(size: Size): Int = size.height

            override fun offset(state: ScrollState): Int = state.offset.y

            override fun paint(canvas: Canvas, position: Int, cell: Cell) = canvas.set(0, position, cell)
        }

        object Horizontal : Axis("▬") {
            override fun track(canvas: Canvas): Int = canvas.width

            override fun length(size: Size): Int = size.width

            override fun offset(state: ScrollState): Int = state.offset.x

            override fun paint(canvas: Canvas, position: Int, cell: Cell) = canvas.set(position, 0, cell)
        }
    }

    public companion object {
        /** A bar for vertical scrolling. */
        public fun vertical(style: Style = Style(StandardRole.PANEL_BORDER)): Scrollbar = Scrollbar(Axis.Vertical, style)

        /** A bar for horizontal scrolling. */
        public fun horizontal(style: Style = Style(StandardRole.PANEL_BORDER)): Scrollbar = Scrollbar(Axis.Horizontal, style)

        /** The thumb's cells within a [track]-cell-long bar, or `null` when the content fits. */
        internal fun thumb(track: Int, contentLength: Int, viewportLength: Int, offset: Int): IntRange? {
            if (track <= 0) return null
            if (contentLength <= viewportLength) return null

            val thumbSize = maxOf(1, track * viewportLength / contentLength)
            val maxOffset = contentLength - viewportLength
            val thumbStart = (offset * (track - thumbSize) + maxOffset / 2) / maxOffset
            return thumbStart until thumbStart + thumbSize
        }
    }
}
