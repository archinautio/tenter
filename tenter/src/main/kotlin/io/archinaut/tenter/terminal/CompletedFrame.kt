// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Rect

/**
 * A read-only view of the cells a [Terminal.draw] call sent to the terminal. It reads the
 * terminal's own buffer, so it is only valid until the next draw.
 */
public class CompletedFrame internal constructor(private val buffer: Buffer) {
    public val width: Int get() = buffer.width
    public val height: Int get() = buffer.height

    /** The area that was drawn. */
    public val area: Rect get() = Rect(0, 0, width, height)

    /** @throws IndexOutOfBoundsException if ([x], [y]) is outside [area]. */
    public fun get(x: Int, y: Int): Cell = buffer.get(x, y)
}
