// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Style

/** Renders a [Style] to the [Tags] a terminal pastes around a run of plain text in that style. */
internal fun interface StyleTags {
    fun tagsFor(style: Style): Tags
}
