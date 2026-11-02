// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas

/** Something that renders into a [Canvas] already clipped to its area, `(0, 0)` being its top-left cell. */
public interface Widget {
    public fun render(canvas: Canvas)
}

/** This plain widget at a fixed [width] and [height], invoked on each paint; it makes no visibility request of its own. */
public fun Widget.fixedSize(width: Int, height: Int): MeasurableWidget {
    require(width >= 0) { "content width must not be negative: $width" }
    require(height >= 0) { "content height must not be negative: $height" }
    val widget = this
    return object : LayoutWidget {
        override fun layout(availableWidth: Int): ContentLayout = ContentLayout.fixed(width, height, widget)
    }
}
