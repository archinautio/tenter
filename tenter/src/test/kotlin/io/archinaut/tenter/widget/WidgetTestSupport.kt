// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Size

/** Renders [widget] into a fresh [width]x[height] buffer and returns it for assertions. */
internal fun render(widget: Widget, width: Int, height: Int): Buffer {
    val buffer = Buffer(width, height)
    widget.render(Canvas.of(buffer))
    return buffer
}

/** Renders [widget] with [state] into a fresh [width]x[height] buffer and returns it for assertions. */
internal fun <S> render(widget: StatefulWidget<S>, width: Int, height: Int, state: S): Buffer {
    val buffer = Buffer(width, height)
    widget.render(Canvas.of(buffer), state)
    return buffer
}

/** This widget's size at [width]. */
internal fun MeasurableWidget.measure(width: Int): Size {
    val layout = prepare(width)
    return Size(layout.width, layout.height)
}

/** Row [y], columns [x] until [x] + [width], right-trimmed. */
internal fun Buffer.line(y: Int, x: Int = 0, width: Int = this.width - x): String =
    (x until x + width).joinToString("") { get(it, y).symbol }.trimEnd()

/** The whole buffer as newline-separated rows — for `contains` assertions. */
internal fun Buffer.text(): String = (0 until height).joinToString("\n") { line(it) }

/** [this] as a plain [Widget] that always renders with [state]. */
internal fun <S> StatefulWidget<S>.bind(state: S): Widget = object : Widget {
    override fun render(canvas: Canvas) {
        this@bind.render(canvas, state)
    }
}
