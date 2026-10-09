// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas

/**
 * Something that renders into a [Canvas], in the manner of Ratatui's `Widget`. The canvas is
 * already clipped and origin-translated to the widget's area: `(0, 0)` is its top-left cell and
 * `canvas.area` is the whole area it may paint.
 */
public interface Widget {
    public fun render(canvas: Canvas)
}
