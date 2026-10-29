// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas

/**
 * Resets its area to blank cells that set no style channel, so a popup rendered after it does not
 * show what lies beneath. A wide glyph straddling its edge is left whole.
 */
public object Clear : Widget {
    override fun render(canvas: Canvas) {
        canvas.clear()
    }
}
