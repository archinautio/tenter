// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/** How a followed request is brought into view along one axis. */
internal fun interface Placement {
    fun target(from: Int, start: Int, end: Int): ScrollTarget

    companion object {
        val FOLLOW: Placement = Placement(ScrollTarget::Follow)

        val CENTER: Placement = Placement { _, start, end -> ScrollTarget.Center(start, end) }
    }
}
