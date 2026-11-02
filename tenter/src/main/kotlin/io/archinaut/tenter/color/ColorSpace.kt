// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/** The space a [TerminalColor] is authored in, one per subtype; a [ColorScheme] uses one for all its roles. */
public enum class ColorSpace {
    /** [TerminalColor.TrueColor], written `#RRGGBB`. */
    TRUECOLOR {
        override fun parse(raw: String): TerminalColor {
            require(raw.length == 7 && raw[0] == '#') { "expected #RRGGBB" }
            val value = raw.substring(1).toInt(16)
            return TerminalColor.TrueColor((value shr 16) and 0xFF, (value shr 8) and 0xFF, value and 0xFF)
        }
    },

    /** [TerminalColor.Ansi256], written as a decimal index in `16..255`. */
    ANSI256 {
        override fun parse(raw: String): TerminalColor = TerminalColor.Ansi256(raw.trim().toInt())
    },

    /** [TerminalColor.Ansi16], written as a decimal SGR code in `30..37` or `90..97`. */
    ANSI16 {
        override fun parse(raw: String): TerminalColor = TerminalColor.Ansi16(raw.trim().toInt())
    },
    ;

    /** Parses [raw] as a color of this space, throwing [IllegalArgumentException] when it is malformed or out of range. */
    public abstract fun parse(raw: String): TerminalColor
}
