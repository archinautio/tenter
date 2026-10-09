// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas

/**
 * Something that renders into a [Canvas] using caller-owned state that outlives one frame, such
 * as a scroll position, in the manner of Ratatui's `StatefulWidget`.
 */
public interface StatefulWidget<in S> {
    public fun render(canvas: Canvas, state: S)
}
