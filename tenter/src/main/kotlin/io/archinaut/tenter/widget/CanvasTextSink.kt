// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

internal class CanvasTextSink(private val canvas: Canvas) : TextSink {
    override val width: Int get() = canvas.width

    override fun write(column: Int, row: Int, text: String, style: Style) {
        canvas.setString(column, row, text, style)
    }

    override fun reveal(rect: Rect) {
        canvas.markReveal(rect)
    }

    override fun place(layout: ContentLayout, x: Int, y: Int) {
        if (layout.width == 0 || layout.height == 0) return
        val stream = Canvas.offscreen(layout.width, layout.height)
        layout.render(stream)
        canvas.blit(stream, x, y)
        stream.revealRect()?.let { reveal ->
            canvas.markReveal(reveal.offset(x, y))
        }
    }

}
