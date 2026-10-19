// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.palette

/**
 * The semantic roles tenter's widgets (chrome, borders, help text, gauges, …) and typical
 * applications (status and text emphasis) draw with. Every [RolePalette] a host application
 * supplies must resolve all of them; an application defines its own [ColorRole] enum for
 * domain-specific roles.
 *
 * Several roles may deliberately resolve to the same value in some palettes — that's a palette
 * choice, not a reason to merge roles here: they are semantically distinct call sites that a
 * palette can later choose to diverge without touching any of them.
 */
public enum class ChromeRole : ColorRole {
    /** The surface's default — the only role whose foreground and background differ. */
    DEFAULT,

    TEXT_PRIMARY,
    TEXT_MUTED,
    TEXT_SUBTLE,
    ACCENT,
    INFO,
    SUCCESS,
    WARNING,
    DANGER,
    DISABLED,
    PANEL_BORDER,
    PANEL_BORDER_FOCUSED,
}
