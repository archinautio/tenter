// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect

/**
 * Scrolls a [MeasurableWidget], painting only the part inside the viewport. The caller owns the
 * [ViewportState] and passes it to every render; read the completed frame from
 * [ViewportState.settled].
 *
 * With a [block], the viewport draws it around the scrolling area, keeps it fixed while the
 * content scrolls, and paints scrollbar thumbs on its right and bottom borders. To make a border
 * scroll away with the content instead, put it on the content: `Viewport(content.block(b))`.
 */
public class Viewport(
    private val content: MeasurableWidget,
    private val block: Block? = null,
) : StatefulWidget<ViewportState> {

    override fun render(canvas: Canvas, state: ViewportState) {
        if (block == null) {
            drawPrepared(canvas, state)
            return
        }
        block.render(canvas)
        drawPrepared(block.inner(canvas), state)
        // Thumbs need the settled offset, so they are painted after the content.
        state.settled?.let { block.drawThumbs(canvas, it) }
    }

    private fun drawPrepared(canvas: Canvas, viewportState: ViewportState) {
        if (canvas.width <= 0 || canvas.height <= 0) return

        val layout = content.prepare(canvas.width)
        val maxOffsetX = (layout.width - canvas.width).coerceAtLeast(0)
        val maxOffsetY = (layout.height - canvas.height).coerceAtLeast(0)
        var offset = ScrollOffset.ZERO
        // The offset depends on the reveal request, so the layout resolves it first and paints
        // only the rows and children inside the window, shifted by the resolved offset.
        val revealed = layout.paintAt(canvas) { reveal ->
            offset = resolveOffset(viewportState, reveal, canvas.width, canvas.height, maxOffsetX, maxOffsetY)
            Point(-offset.x, -offset.y)
        }

        viewportState.settle(
            ScrollState(
                offset = offset,
                maxOffset = ScrollOffset(maxOffsetX, maxOffsetY),
                revealed = revealed,
                viewportWidth = canvas.width,
                viewportHeight = canvas.height,
                contentWidth = layout.width,
                contentHeight = layout.height,
            ),
        )
    }

    private fun resolveOffset(
        viewportState: ViewportState,
        reveal: Rect?,
        viewportWidth: Int,
        viewportHeight: Int,
        maxOffsetX: Int,
        maxOffsetY: Int,
    ): ScrollOffset {
        val follow = viewportState.shouldFollow(reveal, viewportWidth, viewportHeight)
        val base = viewportState.requestedOffset()
        val recenter = viewportState.recenterRequested()
        val offsetX = resolveAxis(
            base = base.x,
            revealRange = reveal?.let { it.x to it.x + it.width },
            viewportSize = viewportWidth,
            maxOffset = maxOffsetX,
            shouldFollow = follow,
            recenter = recenter,
        )
        val offsetY = resolveAxis(
            base = base.y,
            revealRange = reveal?.let { it.y to it.y + it.height },
            viewportSize = viewportHeight,
            maxOffset = maxOffsetY,
            shouldFollow = follow,
            recenter = recenter,
        )
        return ScrollOffset(offsetX, offsetY)
    }

    private fun resolveAxis(
        base: Int,
        revealRange: Pair<Int, Int>?,
        viewportSize: Int,
        maxOffset: Int,
        shouldFollow: Boolean,
        recenter: Boolean,
    ): Int = when {
        revealRange == null -> base.coerceIn(0, maxOffset)
        recenter -> ScrollGeometry.center(revealRange.first, revealRange.second, viewportSize, maxOffset)
        shouldFollow -> ScrollGeometry.follow(base, revealRange.first, revealRange.second, viewportSize, maxOffset)
        else -> base.coerceIn(0, maxOffset)
    }
}
