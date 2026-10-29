// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.StatefulWidget
import io.archinaut.tenter.widget.Widget

/** One frame being drawn by [Screen.draw], valid only inside its callback. */
public class Frame internal constructor(
    private val buffer: Buffer
) {

    /** The full drawable area of this frame. */
    public val area: Rect get() = Rect(0, 0, buffer.width, buffer.height)

    /** A canvas clipped to [area] within this frame. */
    private fun canvas(area: Rect): Canvas = Canvas.of(buffer).region(area)

    /** Renders [widget] into [area]. */
    public fun renderWidget(widget: Widget, area: Rect) {
        widget.render(canvas(area))
    }

    /** Renders [widget] into [area] with the caller-owned [state]. */
    public fun <S> renderStatefulWidget(widget: StatefulWidget<S>, area: Rect, state: S) {
        widget.render(canvas(area), state)
    }
}
