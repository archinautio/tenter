// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

/** Open/close ANSI escape strings for one [Style]; either may be empty. */
internal class Tags(internal val open: String, internal val close: String) {
    internal companion object {
        /** Emits nothing on either side. */
        internal val NONE: Tags = Tags("", "")
    }
}
