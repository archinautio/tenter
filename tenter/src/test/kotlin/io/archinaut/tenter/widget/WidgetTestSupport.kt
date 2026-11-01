// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.terminal.CompletedFrame

/** Renders [widget] into a fresh [width]x[height] frame and returns it for assertions. */
internal fun render(widget: Widget, width: Int, height: Int): CompletedFrame {
    val buffer = Buffer(width, height)
    widget.render(Canvas.of(buffer))
    return CompletedFrame(buffer)
}

/** Renders [widget] with [state] into a fresh [width]x[height] frame and returns it for assertions. */
internal fun <S> render(widget: StatefulWidget<S>, width: Int, height: Int, state: S): CompletedFrame {
    val buffer = Buffer(width, height)
    widget.render(Canvas.of(buffer), state)
    return CompletedFrame(buffer)
}

/** This widget's size at [width]. */
internal fun MeasurableWidget.measure(width: Int): Size {
    val layout = prepare(width)
    return Size(layout.width, layout.height)
}

/** [this] as a plain [Widget] that always renders with [state]. */
internal fun <S> StatefulWidget<S>.bind(state: S): Widget = object : Widget {
    override fun render(canvas: Canvas) {
        this@bind.render(canvas, state)
    }
}
