// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

import com.github.ajalt.mordant.rendering.AnsiLevel

/**
 * A concrete color, in whatever space it was authored in. It is also a [ColorRole] that resolves to
 * itself: use one directly in a [io.archinaut.tenter.screen.Style] for content whose color must not
 * change with the theme; use a semantic role for themeable content.
 *
 * Colors are never converted between spaces: a color scheme picks one [TerminalColor] subtype for every
 * role it defines, matching the terminal capability it targets, and the terminal renders each
 * subtype with the matching ANSI encoding directly. There is no nearest-color downsampling
 * anywhere in this package.
 */
public sealed interface TerminalColor : ColorRole {
    /** 24-bit color for a truecolor-capable terminal. Channels in `0..255`. */
    public data class TrueColor(val red: Int, val green: Int, val blue: Int) : TerminalColor {
        init {
            require(red in 0..255) { "red must be in 0..255, got: $red" }
            require(green in 0..255) { "green must be in 0..255, got: $green" }
            require(blue in 0..255) { "blue must be in 0..255, got: $blue" }
        }
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
    }

    /** ANSI-16 SGR foreground code: `30..37` or `90..97`. Mordant derives the background code from it. */
    public data class Ansi16(val code: Int) : TerminalColor {
        init {
            require(code in 30..37 || code in 90..97) { "code must be in 30..37 or 90..97, got: $code" }
        }
    }

    public companion object {
        /**
         * Parses [raw] as a [TerminalColor] for [level]: `#RRGGBB` for [AnsiLevel.TRUECOLOR], a
         * decimal `16..255` index for [AnsiLevel.ANSI256], a decimal `30..37`/`90..97` code for
         * [AnsiLevel.ANSI16]. Throws [IllegalArgumentException] — same as the subtype
         * constructors' own `require`s — on a malformed or out-of-range value.
         *
         * [AnsiLevel.NONE] has no corresponding [TerminalColor] subtype (there is nothing to
         * render), so it throws rather than silently choosing one of the other three.
         */
        public fun parse(raw: String, level: AnsiLevel): TerminalColor = when (level) {
            AnsiLevel.TRUECOLOR -> parseHex(raw)
            AnsiLevel.ANSI256 -> Ansi256(raw.trim().toInt())
            AnsiLevel.ANSI16 -> Ansi16(raw.trim().toInt())
            AnsiLevel.NONE -> throw IllegalArgumentException("AnsiLevel.NONE has no TerminalColor to parse into")
        }

        private fun parseHex(raw: String): TrueColor {
            require(raw.length == 7 && raw[0] == '#') { "expected #RRGGBB" }
            val value = raw.substring(1).toInt(16)
            return TrueColor((value shr 16) and 0xFF, (value shr 8) and 0xFF, value and 0xFF)
        }
    }
}
