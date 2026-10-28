// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * A [ColorScheme] backed by a plain `Map<ColorRole, TerminalColor>` — the shape [ColorScheme]'s own
 * KDoc anticipates for "a host application's own file-backed color scheme": the role set isn't known
 * until a loader has parsed its source, so completeness is a load-time check on [colors] rather
 * than the compile-time exhaustiveness a hand-authored `when`-based color scheme gets. [space] is the
 * [ColorSpace] [colors] were authored in; every value in [colors] and [defaultBackground] must be in
 * it (never converted between spaces — see [TerminalColor]'s KDoc).
 *
 * Every [StandardRole] is required at construction. Domain-role completeness remains with the
 * application's loader, which knows the application's role set. The source map is copied, so
 * later mutation by a loader or caller cannot change a color scheme after a terminal has cached it.
 */
public class MapColorScheme(
    private val name: String,
    public val space: ColorSpace,
    override val defaultBackground: TerminalColor,
    colors: Map<ColorRole, TerminalColor>,
) : ColorScheme {

    private val colors: Map<ColorRole, TerminalColor> = colors.toMap()

    init {
        val missingStandardRoles = StandardRole.entries.filterNot(colors.keys::contains)
        require(missingStandardRoles.isEmpty()) {
            "Color scheme $name is missing standard roles: ${missingStandardRoles.joinToString(", ")}"
        }
        require(defaultBackground.space == space) {
            "Color scheme $name default background must use $space colors, got $defaultBackground"
        }
        require(colors.values.all { it.space == space }) {
            "Color scheme $name contains a color that does not use $space"
        }
    }

    override fun foreground(role: ColorRole): TerminalColor =
        colors[role] ?: error("Unknown color role: $role")

    /** [name] — the color scheme's source name (a built-in stem, or a custom file path), for readable test/log output. */
    override fun toString(): String = name
}
