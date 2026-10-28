// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * A concrete color, in whatever space it was authored in. It is also a [ColorRole] that resolves to
 * itself: use one directly in a [io.archinaut.tenter.screen.Style] for content whose color must not
 * change with the theme; use a semantic role for themeable content.
 *
 * Colors are never converted between spaces: a color scheme picks one [ColorSpace] for every role it
 * defines, matching the terminal capability it targets, and the terminal renders each subtype with
 * the matching ANSI encoding directly. There is no nearest-color downsampling anywhere in this
 * package. [ColorSpace.parse] reads a color from text.
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

    /**
     * xterm-256 palette index, restricted to `16..255`. Indices `0..15` are remapped by the
     * user's terminal theme, so their contrast is unknowable at build time — `16..255` are fixed
     * by the xterm specification and therefore both reproducible and testable.
     */
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
