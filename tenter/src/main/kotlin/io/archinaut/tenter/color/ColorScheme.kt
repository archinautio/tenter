// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * Resolves every [ColorRole] a host application uses — [StandardRole] plus whatever domain-specific
 * roles the application defines — to a [TerminalColor], all in the same color space.
 *
 * Two implementation shapes exist. A color scheme hand-authored in Kotlin as a stateless object with
 * [foreground] a single `when` expression gets its completeness checked at **compile** time —
 * Kotlin's enum-`when` exhaustiveness makes "you forgot a role" a build error in every such
 * implementation (see [ColorRole]'s KDoc for the two-enum `when` shape this relies on). A color scheme
 * loaded from an external source (a host application's own file-backed color scheme, say) is
 * necessarily backed by something map-shaped instead, since the role set isn't known until the
 * source is parsed; that trades the compile-time guarantee for a **load-time** one. [MapColorScheme]
 * validates every toolkit role before construction; an application loader remains responsible for
 * validating its own domain-role set.
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
        /**
         * A conservative ANSI-16 color scheme for consumers that need a working color scheme
         * immediately. ANSI-16 colors are terminal-defined, so the terminal's configured palette
         * determines the exact appearance. A screen scope keeps its color scheme for its lifetime;
         * pass another to a new scope to change roles.
         */
        public val Default: ColorScheme = DefaultColorScheme
    }
}
