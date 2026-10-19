// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect

/**
 * Scrolls prepared content in an exactly sized offscreen stream. The caller owns the
 * [ViewportState] and passes it to every render; read the completed frame from
 * [ViewportState.settled].
 */
public class Viewport(
    private val content: ContentWidget,
) : StatefulWidget<ViewportState> {

    override fun render(canvas: Canvas, state: ViewportState) {
        drawPrepared(canvas, state)
    }

    private fun drawPrepared(canvas: Canvas, viewportState: ViewportState) {
        if (canvas.width <= 0 || canvas.height <= 0) return

        val layout = content.layout(canvas.width)
        val stream = Canvas.offscreen(layout.width, layout.height)
        layout.render(stream)

        val maxOffsetX = (layout.width - canvas.width).coerceAtLeast(0)
        val maxOffsetY = (layout.height - canvas.height).coerceAtLeast(0)
        val reveal = stream.revealRect()
        val follow = viewportState.shouldFollow(reveal, canvas.width, canvas.height)
        val base = viewportState.requestedOffset()
        val recenter = viewportState.recenterRequested()
        val offsetX = resolveAxis(
            base = base.x,
            revealRange = reveal?.let { it.x to it.x + it.width },
            viewportSize = canvas.width,
            maxOffset = maxOffsetX,
            shouldFollow = follow,
            recenter = recenter,
        )
        val offsetY = resolveAxis(
            base = base.y,
            revealRange = reveal?.let { it.y to it.y + it.height },
            viewportSize = canvas.height,
            maxOffset = maxOffsetY,
            shouldFollow = follow,
            recenter = recenter,
        )

        canvas.blit(stream.region(Rect(offsetX, offsetY, canvas.width, canvas.height)))
        viewportState.settle(
            ScrollState(
                offset = ScrollOffset(offsetX, offsetY),
                maxOffset = ScrollOffset(maxOffsetX, maxOffsetY),
                revealed = reveal,
                viewportWidth = canvas.width,
                viewportHeight = canvas.height,
                contentWidth = layout.width,
                contentHeight = layout.height,
            ),
        )
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
