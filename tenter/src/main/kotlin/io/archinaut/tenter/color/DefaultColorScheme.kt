// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/** The ANSI-16 color scheme behind [ColorScheme.Default]. */
internal object DefaultColorScheme : ColorScheme {

    override val defaultBackground: TerminalColor = TerminalColor.Ansi16(30)

    override fun foreground(role: ColorRole): TerminalColor = when (role) {
        StandardRole.DEFAULT, StandardRole.TEXT_PRIMARY -> TerminalColor.Ansi16(97)
        StandardRole.TEXT_MUTED, StandardRole.PANEL_BORDER -> TerminalColor.Ansi16(37)
        StandardRole.TEXT_SUBTLE, StandardRole.DISABLED -> TerminalColor.Ansi16(90)
        StandardRole.ACCENT, StandardRole.WARNING -> TerminalColor.Ansi16(93)
        StandardRole.INFO -> TerminalColor.Ansi16(96)
        StandardRole.SUCCESS, StandardRole.PANEL_BORDER_FOCUSED -> TerminalColor.Ansi16(92)
        StandardRole.DANGER -> TerminalColor.Ansi16(91)
        else -> error("ColorScheme.Default only resolves StandardRole, got: $role")
    }

    override fun toString(): String = "ColorScheme.Default"
}
