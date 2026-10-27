// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.color.ColorRole

/**
 * Foreground and background roles plus text attributes for a [Cell]. A null channel is unset: a
 * write leaves that channel of the painted cell as it is (see [patch]), so a glyph drawn with only
 * a foreground keeps the background already under it. A channel no write ever set renders as
 * [io.archinaut.tenter.color.StandardRole.DEFAULT], or without strikethrough.
 */
public data class Style(
    val fg: ColorRole? = null,
    val bg: ColorRole? = null,
    val strikethrough: Boolean? = null,
) {
    /** This style with every channel [other] sets replaced by [other]'s. */
    public fun patch(other: Style): Style = when {
        // Every cell write patches, so the two common cases avoid allocating.
        other == DEFAULT -> this
        other.fg != null && other.bg != null && other.strikethrough != null -> other
        else -> Style(
            fg = other.fg ?: fg,
            bg = other.bg ?: bg,
            strikethrough = other.strikethrough ?: strikethrough,
        )
    }

    public companion object {
        /** The style that sets no channel; reused everywhere an unstyled cell is needed. */
        public val DEFAULT: Style = Style()
    }
}
