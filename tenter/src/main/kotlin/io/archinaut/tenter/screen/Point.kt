// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/** An immutable cell coordinate local to a screen or content rectangle, such as a scroll offset. */
public data class Point(
    public val x: Int,
    public val y: Int,
) {
    public companion object {
        /** The top-left cell, `(0, 0)`. */
        public val ORIGIN: Point = Point(0, 0)
    }
}
