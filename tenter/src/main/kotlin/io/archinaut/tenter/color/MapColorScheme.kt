// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

import com.github.ajalt.mordant.rendering.AnsiLevel

/**
 * A [ColorScheme] backed by a plain `Map<ColorRole, TerminalColor>` — the shape [ColorScheme]'s own
 * KDoc anticipates for "a host application's own file-backed color scheme": the role set isn't known
 * until a loader has parsed its source, so completeness is a load-time check on [colors] rather
 * than the compile-time exhaustiveness a hand-authored `when`-based color scheme gets. [level] is the
 * [AnsiLevel] tier [colors] were authored for; every value in [colors] is a [TerminalColor] of the
 * matching subtype (never converted between tiers — see [TerminalColor]'s KDoc).
 *
 * Every [StandardRole] is required at construction. Domain-role completeness remains with the
 * application's loader, which knows the application's role set. The source map is copied, so
 * later mutation by a loader or caller cannot change a color scheme after a terminal has cached it.
 *
 * [AnsiLevel.NONE] is not an authoring tier and is rejected: it names a terminal that cannot show
 * color, not a set of color values. Author for the tier the colors use; [io.archinaut.tenter.terminal.Terminal] drops
 * every SGR tag on its own when the terminal reports NONE.
 */
public class MapColorScheme(
    private val name: String,
    public val level: AnsiLevel,
    override val defaultBackground: TerminalColor,
    colors: Map<ColorRole, TerminalColor>,
) : ColorScheme {

    private val colors: Map<ColorRole, TerminalColor> = colors.toMap()

    init {
        require(level != AnsiLevel.NONE) {
            "Color scheme $name cannot be authored for AnsiLevel.NONE, which has no color values of " +
                "its own. Author it for the tier the color scheme's colors use; the Terminal " +
                "suppresses every SGR tag when the terminal itself reports NONE."
        }
        val missingStandardRoles = StandardRole.entries.filterNot(colors.keys::contains)
        require(missingStandardRoles.isEmpty()) {
            "Color scheme $name is missing standard roles: ${missingStandardRoles.joinToString(", ")}"
        }
        require(isCorrectTier(defaultBackground)) {
            "Color scheme $name default background must use $level colors, got $defaultBackground"
        }
        require(colors.values.all(::isCorrectTier)) {
            "Color scheme $name contains a color that does not use $level"
        }
    }

    override fun foreground(role: ColorRole): TerminalColor =
        colors[role] ?: error("Unknown color role: $role")

    /** [name] — the color scheme's source name (a built-in stem, or a custom file path), for readable test/log output. */
    override fun toString(): String = name

    private fun isCorrectTier(color: TerminalColor): Boolean = when (level) {
        AnsiLevel.TRUECOLOR -> color is TerminalColor.TrueColor
        AnsiLevel.ANSI256 -> color is TerminalColor.Ansi256
        AnsiLevel.ANSI16 -> color is TerminalColor.Ansi16
        AnsiLevel.NONE -> false
    }
}
