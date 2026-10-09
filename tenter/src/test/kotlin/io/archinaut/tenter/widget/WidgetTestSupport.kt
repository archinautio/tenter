// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Buffer

/** Renders [widget] into a fresh [width]x[height] buffer and returns it for assertions. */
public fun render(widget: Widget, width: Int, height: Int): Buffer {
    val buffer = Buffer(width, height)
    widget.render(Canvas.of(buffer))
    return buffer
}

/** Renders [widget] with [state] into a fresh [width]x[height] buffer and returns it for assertions. */
public fun <S> render(widget: StatefulWidget<S>, width: Int, height: Int, state: S): Buffer {
    val buffer = Buffer(width, height)
    widget.render(Canvas.of(buffer), state)
    return buffer
}

/** Renders [content] inside the real [scrollingPanel] chrome — the pixel-parity regression guard. */
public fun renderInPanel(
    content: ContentWidget,
    badge: Char = '0',
    title: String = "T",
    width: Int = 28,
    height: Int = 30,
    scrollOffset: Int? = 0,
): Buffer = render(
    scrollingPanel(
        title = title,
        badge = badge.toString(),
        content = content,
    ),
    width,
    height,
    ViewportState(scrollOffset?.let { ScrollOffset(y = it) } ?: ScrollOffset.ZERO),
)

/** Row [y], columns [x] until [x] + [width], right-trimmed. */
public fun Buffer.line(y: Int, x: Int = 0, width: Int = this.width - x): String =
    (x until x + width).joinToString("") { get(it, y).symbol }.trimEnd()

/** The whole buffer as newline-separated rows — for `contains` assertions. */
public fun Buffer.text(): String = (0 until height).joinToString("\n") { line(it) }

/** [this] as a plain [Widget] that always renders with [state]. */
public fun <S> StatefulWidget<S>.bind(state: S): Widget = object : Widget {
    override fun render(canvas: Canvas) {
        this@bind.render(canvas, state)
    }
}
