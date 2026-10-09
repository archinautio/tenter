// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/** One terminal cell: a drawable grapheme [symbol] painted in [style]. */
public data class Cell(
    val symbol: String = " ",
    val style: Style = Style.DEFAULT,
) {
    public companion object {
        /** The default, unstyled space cell — reused wherever a blank cell is needed so a fresh
         * screen doesn't allocate a distinct identical instance per cell. */
        public val EMPTY: Cell = Cell()
    }
}
