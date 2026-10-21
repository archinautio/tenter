// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/**
 * Which sides of a [Block] draw a border. Combine sides with `+`, e.g.
 * `Borders.TOP + Borders.BOTTOM`.
 */
public class Borders private constructor(private val sides: Int) {

    internal val top: Boolean get() = sides and TOP_BIT != 0
    internal val bottom: Boolean get() = sides and BOTTOM_BIT != 0
    internal val left: Boolean get() = sides and LEFT_BIT != 0
    internal val right: Boolean get() = sides and RIGHT_BIT != 0

    public operator fun plus(other: Borders): Borders = Borders(sides or other.sides)

    override fun equals(other: Any?): Boolean = other is Borders && other.sides == sides

    override fun hashCode(): Int = sides

    override fun toString(): String = when (this) {
        NONE -> "Borders.NONE"
        ALL -> "Borders.ALL"
        else -> listOfNotNull(
            "TOP".takeIf { top },
            "BOTTOM".takeIf { bottom },
            "LEFT".takeIf { left },
            "RIGHT".takeIf { right },
        ).joinToString(" + ", prefix = "Borders(", postfix = ")")
    }

    public companion object {
        private const val TOP_BIT = 1
        private const val BOTTOM_BIT = 2
        private const val LEFT_BIT = 4
        private const val RIGHT_BIT = 8

        public val NONE: Borders = Borders(0)
        public val TOP: Borders = Borders(TOP_BIT)
        public val BOTTOM: Borders = Borders(BOTTOM_BIT)
        public val LEFT: Borders = Borders(LEFT_BIT)
        public val RIGHT: Borders = Borders(RIGHT_BIT)
        public val ALL: Borders = Borders(TOP_BIT or BOTTOM_BIT or LEFT_BIT or RIGHT_BIT)
    }
}
