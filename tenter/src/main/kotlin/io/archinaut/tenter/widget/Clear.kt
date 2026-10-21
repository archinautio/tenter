// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas

/**
 * Resets its whole area to blank cells that set no style channel, as if nothing had been painted
 * there. Writes patch what is already painted (see [io.archinaut.tenter.screen.Style.patch]), so
 * render this first to keep a popup — a dialog, a banner — from showing what lies beneath it:
 * `frame.renderWidget(Clear, area)` then the popup into the same area.
 *
 * Like every write, it leaves whole a wide glyph that straddles the edge of its area.
 */
public object Clear : Widget {
    override fun render(canvas: Canvas) {
        canvas.clear()
    }
}
