// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size

/**
 * Scrolls a [MeasurableWidget] across the whole canvas, painting only the part inside it. It is
 * the only widget that clips content: composites grow to fit what they hold. The caller owns the
 * [ViewportState] and passes it to every render; read the completed frame from
 * [ViewportState.settled].
 *
 * A viewport draws no chrome. Frame it by rendering a [Block] and then the viewport into
 * [Block.inner], and show its position with a [Scrollbar] rendered from the settled state.
 */
public class Viewport(private val content: MeasurableWidget) : StatefulWidget<ViewportState> {

    override fun render(canvas: Canvas, state: ViewportState) {
        if (canvas.width <= 0 || canvas.height <= 0) return

        val layout = content.prepare(canvas.width)
        val viewport = Size(canvas.width, canvas.height)
        val maxOffset = Point(
            (layout.width - canvas.width).coerceAtLeast(0),
            (layout.height - canvas.height).coerceAtLeast(0),
        )
        val request = layout.request()
        val offset = state.targets(request, viewport).offset(viewport, maxOffset)
        layout.paint(canvas.window(Rect(-offset.x, -offset.y, layout.width, layout.height)))

        state.settle(
            ScrollState(
                offset = offset,
                maxOffset = maxOffset,
                viewport = viewport,
                content = Size(layout.width, layout.height),
                revealed = request?.area,
            ),
        )
    }
}
