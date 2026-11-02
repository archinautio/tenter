// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * The semantic roles Tenter's widgets and typical applications draw with; every [ColorScheme] must
 * resolve all of them. Roles that share a color in one scheme stay distinct, so another can differ.
 */
public enum class StandardRole : ColorRole {
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
