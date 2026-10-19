// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/** A nonnegative width and height in terminal cells, in the manner of Ratatui's `Size`. */
public data class Size(
    public val width: Int,
    public val height: Int,
) {
    init {
        require(width >= 0) { "width must not be negative: $width" }
        require(height >= 0) { "height must not be negative: $height" }
    }
}
