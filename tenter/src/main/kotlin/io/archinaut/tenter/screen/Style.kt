// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.color.ColorRole

/**
 * The change a write applies to a [Cell]: the foreground and background roles and text attributes
 * it sets. A null channel is unset, so the write keeps what the cell showed; set a channel to
 * replace it, including back to [io.archinaut.tenter.color.StandardRole.DEFAULT] or off.
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
        // The two common cases avoid allocating.
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

    private fun setsEveryChannel(): Boolean =
        fg != null && bg != null && bold != null && dim != null && italic != null &&
            underline != null && inverse != null && strikethrough != null

    public companion object {
        /** The style that sets no channel. */
        public val DEFAULT: Style = Style()
    }
}
