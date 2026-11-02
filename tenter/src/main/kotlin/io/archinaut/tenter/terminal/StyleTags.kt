// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Cell

/** The [Tags] a terminal pastes around a run of plain text shown as [Cell]s that look like [cell]. */
internal fun interface StyleTags {
    fun tagsFor(cell: Cell): Tags
}
