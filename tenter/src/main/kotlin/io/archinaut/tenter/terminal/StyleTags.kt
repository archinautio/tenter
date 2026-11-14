// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Cell

/** The [Tags] a terminal uses around a run of text whose cells share the given cell's appearance. */
internal fun interface StyleTags {
    fun tagsFor(cell: Cell): Tags
}
