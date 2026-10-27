// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.color.ColorRole

/**
 * Foreground and background roles plus text attributes for a [Cell]. A null channel is unset: a
 * write leaves that channel of the painted cell as it is (see [patch]), so a glyph drawn with only
 * a foreground keeps the background already under it. A channel no write ever set renders as
 * [io.archinaut.tenter.color.StandardRole.DEFAULT], or with the attribute off.
 *
 * Attributes render through Mordant's `TextStyle`, so each one looks however the terminal draws
 * that SGR attribute; some terminals ignore [italic] or render [dim] like normal intensity.
 */
public data class Style(
    val fg: ColorRole? = null,
    val bg: ColorRole? = null,
    val bold: Boolean? = null,
    val dim: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val inverse: Boolean? = null,
    val strikethrough: Boolean? = null,
) {
    /** This style with every channel [other] sets replaced by [other]'s. */
    public fun patch(other: Style): Style = when {
        // Every cell write patches, so the two common cases avoid allocating.
        other == DEFAULT -> this
        other.setsEveryChannel() -> other
        else -> Style(
            fg = other.fg ?: fg,
            bg = other.bg ?: bg,
            bold = other.bold ?: bold,
            dim = other.dim ?: dim,
            italic = other.italic ?: italic,
            underline = other.underline ?: underline,
            inverse = other.inverse ?: inverse,
            strikethrough = other.strikethrough ?: strikethrough,
        )
    }

    /** Whether this style and [other] render the same attributes, treating an unset attribute as off. */
    internal fun rendersSameAttributes(other: Style): Boolean =
        (bold ?: false) == (other.bold ?: false) &&
            (dim ?: false) == (other.dim ?: false) &&
            (italic ?: false) == (other.italic ?: false) &&
            (underline ?: false) == (other.underline ?: false) &&
            (inverse ?: false) == (other.inverse ?: false) &&
            (strikethrough ?: false) == (other.strikethrough ?: false)

    private fun setsEveryChannel(): Boolean =
        fg != null && bg != null && bold != null && dim != null && italic != null &&
            underline != null && inverse != null && strikethrough != null

    public companion object {
        /** The style that sets no channel; reused everywhere an unstyled cell is needed. */
        public val DEFAULT: Style = Style()
    }
}
