// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.colors

import io.archinaut.tenter.color.ColorScheme
import io.archinaut.tenter.color.ColorSpace
import io.archinaut.tenter.color.MapColorScheme
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.color.TerminalColor

/** The showcase's color schemes, one per [ColorSpace], each built a different way. */
internal enum class ShowcaseScheme(
    /** The name `--scheme` takes. */
    internal val label: String,
    internal val space: ColorSpace,
    internal val description: String,
) {
    ANSI16("ansi16", ColorSpace.ANSI16, "ColorScheme.Default") {
        override fun build(): ColorScheme = ColorScheme.Default
    },
    ANSI256("ansi256", ColorSpace.ANSI256, "MapColorScheme in Kotlin") {
        override fun build(): ColorScheme = MapColorScheme(
            name = label,
            space = space,
            defaultBackground = TerminalColor.Ansi256(235),
            colors = mapOf(
                StandardRole.DEFAULT to TerminalColor.Ansi256(252),
                StandardRole.TEXT_PRIMARY to TerminalColor.Ansi256(255),
                StandardRole.TEXT_MUTED to TerminalColor.Ansi256(246),
                StandardRole.TEXT_SUBTLE to TerminalColor.Ansi256(241),
                StandardRole.ACCENT to TerminalColor.Ansi256(214),
                StandardRole.INFO to TerminalColor.Ansi256(74),
                StandardRole.SUCCESS to TerminalColor.Ansi256(114),
                StandardRole.WARNING to TerminalColor.Ansi256(221),
                StandardRole.DANGER to TerminalColor.Ansi256(203),
                StandardRole.DISABLED to TerminalColor.Ansi256(238),
                StandardRole.PANEL_BORDER to TerminalColor.Ansi256(243),
                StandardRole.PANEL_BORDER_FOCUSED to TerminalColor.Ansi256(141),
            ),
        )
    },
    TRUECOLOR("truecolor", ColorSpace.TRUECOLOR, "parsed #RRGGBB theme") {
        override fun build(): ColorScheme {
            // Each line is "role = #RRGGBB"; "background" names the default background.
            val entries = TRUECOLOR_THEME.lines().filter(String::isNotBlank).associate { line ->
                val (key, value) = line.split('=').map(String::trim)
                key to space.parse(value)
            }
            return MapColorScheme(
                name = label,
                space = space,
                defaultBackground = entries.getValue("background"),
                colors = (entries - "background").mapKeys { (key, _) -> StandardRole.valueOf(key.uppercase()) },
            )
        }
    },
    ;

    internal abstract fun build(): ColorScheme

    /** Whether this scheme uses more colors than a terminal showing [terminal] can, null being none. */
    internal fun exceeds(terminal: ColorSpace?): Boolean = terminal == null || ordinal > forSpace(terminal).ordinal

    internal companion object {
        /** The scheme matching what a terminal shows, the ANSI-16 one when it shows no color. */
        internal fun forTerminal(terminal: ColorSpace?): ShowcaseScheme = terminal?.let(::forSpace) ?: ANSI16

        /** The scheme named [label], or null. */
        internal fun named(label: String): ShowcaseScheme? = entries.firstOrNull { it.label == label }

        private fun forSpace(space: ColorSpace): ShowcaseScheme = entries.first { it.space == space }

        private val TRUECOLOR_THEME: String = """
            background = #1a1b26
            default = #c0caf5
            text_primary = #c0caf5
            text_muted = #9aa5ce
            text_subtle = #565f89
            accent = #ff9e64
            info = #7dcfff
            success = #9ece6a
            warning = #e0af68
            danger = #f7768e
            disabled = #414868
            panel_border = #3b4261
            panel_border_focused = #bb9af7
        """.trimIndent()
    }
}
