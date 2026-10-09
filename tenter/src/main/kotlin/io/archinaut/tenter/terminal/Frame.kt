// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.StatefulWidget
import io.archinaut.tenter.widget.Widget

/**
 * One frame being drawn by [Terminal.draw], in the manner of Ratatui's `Frame`. It is only valid
 * inside the draw callback that received it.
 *
 * Widgets render into a [Rect] of [area]; each one receives a [Canvas] clipped to the
 * intersection of that rectangle with the frame, so it cannot paint outside its own area.
 */
public class Frame internal constructor(
    /** The buffer being drawn, for direct cell access. Prefer [renderWidget]. */
    public val buffer: Buffer,
) {
    /** The full drawable area of this frame. Split it with [io.archinaut.tenter.screen.Layout]. */
    public val area: Rect get() = Rect(0, 0, buffer.width, buffer.height)

    /** A canvas clipped to [area] within this frame, for APIs that paint a canvas directly. */
    public fun canvas(area: Rect = this.area): Canvas = Canvas.of(buffer).region(area)

    /** Renders [widget] into [area]. */
    public fun renderWidget(widget: Widget, area: Rect) {
        widget.render(canvas(area))
    }

    /** Renders [widget] into [area] with the caller-owned [state]. */
    public fun <S> renderStatefulWidget(widget: StatefulWidget<S>, area: Rect, state: S) {
        widget.render(canvas(area), state)
    }
}
