// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas

/** Explicitly dimensioned empty content for layout tests. */
internal object EmptyWidget : Widget {
    public override fun render(canvas: Canvas): Unit = Unit
}
