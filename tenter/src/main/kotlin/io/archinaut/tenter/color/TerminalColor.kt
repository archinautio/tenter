// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * A concrete color in the space it was authored in, never converted to another. As a [ColorRole]
 * it resolves to itself, for content whose color must not change with the theme.
 */
public sealed interface TerminalColor : ColorRole {
    /** The space this color is authored in. */
    public val space: ColorSpace

    /** 24-bit color for a truecolor-capable terminal. Channels in `0..255`. */
    public data class TrueColor(val red: Int, val green: Int, val blue: Int) : TerminalColor {
        init {
            require(red in 0..255) { "red must be in 0..255, got: $red" }
            require(green in 0..255) { "green must be in 0..255, got: $green" }
            require(blue in 0..255) { "blue must be in 0..255, got: $blue" }
        }

        override val space: ColorSpace get() = ColorSpace.TRUECOLOR
    }

    /** xterm-256 palette index in `16..255`; `0..15` are excluded because terminal themes remap them. */
    public data class Ansi256(val index: Int) : TerminalColor {
        init {
            require(index in 16..255) { "index must be in 16..255, got: $index" }
        }

        override val space: ColorSpace get() = ColorSpace.ANSI256
    }

    /** ANSI-16 SGR foreground code: `30..37` or `90..97`. Mordant derives the background code from it. */
    public data class Ansi16(val code: Int) : TerminalColor {
        init {
            require(code in 30..37 || code in 90..97) { "code must be in 30..37 or 90..97, got: $code" }
        }

        override val space: ColorSpace get() = ColorSpace.ANSI16
    }
}
