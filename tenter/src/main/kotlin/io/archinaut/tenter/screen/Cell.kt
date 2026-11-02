// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.color.StandardRole

/**
 * What one terminal cell shows: a drawable grapheme [symbol] in its colors and text attributes.
 * Every value is concrete, so two cells that look alike are equal. A write changes a cell by
 * applying a [Style] to it.
 */
public data class Cell(
    public val symbol: String = " ",
    public val fg: ColorRole = StandardRole.DEFAULT,
    public val bg: ColorRole = StandardRole.DEFAULT,
    public val bold: Boolean = false,
    public val dim: Boolean = false,
    public val italic: Boolean = false,
    public val underline: Boolean = false,
    public val inverse: Boolean = false,
    public val strikethrough: Boolean = false,
) {

    /** The six attributes as bits, so comparing or keying them costs one `Int`. */
    internal val attributes: Int =
        (if (bold) BOLD else 0) or
            (if (dim) DIM else 0) or
            (if (italic) ITALIC else 0) or
            (if (underline) UNDERLINE else 0) or
            (if (inverse) INVERSE else 0) or
            (if (strikethrough) STRIKETHROUGH else 0)

    /** This cell showing [symbol], with every channel [style] sets replaced by [style]'s. */
    internal fun painted(symbol: String, style: Style): Cell {
        // Most writes set no channel, so they keep this cell's look without building a new one.
        if (style == Style.DEFAULT) return if (symbol == this.symbol) this else copy(symbol = symbol)
        return Cell(
            symbol = symbol,
            fg = style.fg ?: fg,
            bg = style.bg ?: bg,
            bold = style.bold ?: bold,
            dim = style.dim ?: dim,
            italic = style.italic ?: italic,
            underline = style.underline ?: underline,
            inverse = style.inverse ?: inverse,
            strikethrough = style.strikethrough ?: strikethrough,
        )
    }

    /** Whether [other] shows its symbol in the same colors and attributes. */
    internal fun looksLike(other: Cell): Boolean = fg == other.fg && bg == other.bg && attributes == other.attributes

    internal companion object {
        /** The blank cell in the default colors with no attribute. */
        internal val EMPTY: Cell = Cell()

        private const val BOLD: Int = 1
        private const val DIM: Int = 2
        private const val ITALIC: Int = 4
        private const val UNDERLINE: Int = 8
        private const val INVERSE: Int = 16
        private const val STRIKETHROUGH: Int = 32
    }
}
