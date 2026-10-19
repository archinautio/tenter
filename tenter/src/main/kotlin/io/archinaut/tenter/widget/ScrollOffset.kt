// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/** A 2D scroll offset, in characters. */
public data class ScrollOffset(
    public val x: Int = 0,
    public val y: Int = 0,
) {
    public companion object {
        public val ZERO: ScrollOffset = ScrollOffset()
    }
}
