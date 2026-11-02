// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * A [ColorScheme] backed by a copied map, for colors loaded from data. Every [StandardRole] is
 * required, and every color must be in [space]; checking the application's own roles is its
 * loader's job.
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

    /** [name], for readable output. */
    override fun toString(): String = name
}
