// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * A color role. A semantic role resolves to a concrete color through a [ColorScheme], so a theme
 * can change it; a [TerminalColor] is a role that resolves to itself, for fixed colors.
 *
 * [ColorRole] is a pure marker — tenter defines no closed set of every role that can exist.
 * [StandardRole] is the set tenter's own widgets draw with; a host application defines its own
 * `ColorRole` enum for anything domain-specific (e.g. a map's terrain or faction colors) and
 * implements [ColorScheme.foreground] as an exhaustive `when` over both enums:
 *
 * ```
 * override fun foreground(role: ColorRole): TerminalColor = when (role) {
 *     is StandardRole -> ...       // exhaustive over tenter's roles
 *     is MyAppRole -> ...    // exhaustive over the app's own roles
 *     else -> error("unknown color role: $role")
 * }
 * ```
 *
 * Kotlin's exhaustiveness check still applies to each `is` branch's inner `when`, so adding a
 * role to either enum is still a compile error in every color scheme that doesn't handle it.
 */
public interface ColorRole
