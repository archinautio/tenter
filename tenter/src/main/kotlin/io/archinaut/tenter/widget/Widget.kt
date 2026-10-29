// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas

/** Something that renders into a [Canvas] already clipped to its area, `(0, 0)` being its top-left cell. */
public interface Widget {
    public fun render(canvas: Canvas)
}
