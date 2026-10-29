// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.widget.Widget

/** Draws a frame whose cells are a copy of [buffer], resizing the terminal to match it first. */
internal fun Screen.renderBuffer(buffer: Buffer): CompletedFrame {
    terminal.resize(buffer.width, buffer.height)
    return draw { frame ->
        frame.renderWidget(
            object : Widget {
                override fun render(canvas: Canvas) {
                    for (y in 0 until buffer.height) {
                        for (x in 0 until buffer.width) {
                            if (buffer.glyphStart(x, y) == x) canvas.set(x, y, buffer.get(x, y))
                        }
                    }
                }
            },
            frame.area,
        )
    }
}
