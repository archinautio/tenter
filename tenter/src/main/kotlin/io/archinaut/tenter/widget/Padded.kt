// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Padding

/** Padding raw painting within its allocated canvas. Use [prepared] for intrinsic composition. */
public class Padded(
    private val padding: Padding,
    private val content: Widget,
) : Widget {
    public override fun render(canvas: Canvas) {
        content.render(canvas.inner(padding))
    }

    public companion object {
        /** Adds logical padding to prepared content, including otherwise blank space. */
        public fun prepared(padding: Padding, content: ContentWidget): ContentWidget = object : ContentWidget {
            public override fun layout(availableWidth: Int): ContentLayout {
                require(availableWidth >= 0) { "available width must not be negative: $availableWidth" }
                val innerWidth = (availableWidth.toLong() - padding.left - padding.right).coerceAtLeast(0).toInt()
                val child = content.layout(innerWidth)
                val width = checkedAdd(checkedAdd(child.width, padding.left, PADDED), padding.right, PADDED)
                val height = checkedAdd(checkedAdd(child.height, padding.top, PADDED), padding.bottom, PADDED)
                return contentLayout(width, height) {
                    place(padding.left, padding.top, child)
                }
            }
        }

        private const val PADDED: String = "padded size"
    }
}
