// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Style

/** Renders a [Style] to the [Tags] a terminal pastes around a run of plain text in that style. */
internal fun interface StyleTags {
    fun tagsFor(style: Style): Tags
}

/** Open/close ANSI escape strings for one [Style]; either may be empty. */
internal class Tags(internal val open: String, internal val close: String) {
    internal companion object {
        /** Emits nothing on either side. */
        internal val NONE: Tags = Tags("", "")
    }
}

/** The [StyleTags] of a terminal that shows no color: no style emits an escape. */
internal object NoStyleTags : StyleTags {
    override fun tagsFor(style: Style): Tags = Tags.NONE
}
