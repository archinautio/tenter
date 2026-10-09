// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas

/** A width-constrained prepared content source. */
public interface ContentWidget : Widget {
    /**
     * Prepares content for a nonnegative available width. Logical width may exceed that width
     * for fixed content. Capture frame data here; do not use painting as a measurement pass.
     * The returned layout may be painted repeatedly (see [ContentLayout]).
     */
    public fun layout(availableWidth: Int): ContentLayout

    override fun render(canvas: Canvas) {
        layout(canvas.width).render(canvas)
    }
}
