// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * Resolves every [ColorRole] an application uses, [StandardRole] and its own, to a [TerminalColor]
 * in one color space. Implemented with an exhaustive `when` over the role enums, a missing role is
 * a compile error; [MapColorScheme] checks [StandardRole] when it is built instead.
 */
public interface ColorScheme {
    /** This color scheme's default background. [foreground] of [StandardRole.DEFAULT] is the default *foreground*. */
    public val defaultBackground: TerminalColor

    /**
     * [role]'s semantic foreground. For [StandardRole.DEFAULT] this is the default foreground — the
     * default role where [foreground] and [background] differ. Implementations may specialize other
     * backgrounds too. A [TerminalColor] used as a role is resolved by the terminal before this
     * function is called, not by a color scheme implementation.
     */
    public fun foreground(role: ColorRole): TerminalColor

    /** [role]'s background. For [StandardRole.DEFAULT] this is [defaultBackground], not [foreground]'s value. */
    public fun background(role: ColorRole): TerminalColor =
        if (role == StandardRole.DEFAULT) defaultBackground else foreground(role)

    public companion object {
        /** An ANSI-16 color scheme, so the terminal's configured palette decides its exact appearance. */
        public val Default: ColorScheme = DefaultColorScheme
    }
}
