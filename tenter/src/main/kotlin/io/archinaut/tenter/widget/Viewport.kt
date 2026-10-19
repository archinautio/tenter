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
 */
public class Viewport(
    private val content: MeasurableWidget,
) : StatefulWidget<ViewportState> {

    override fun render(canvas: Canvas, state: ViewportState) {
        drawPrepared(canvas, state)
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
