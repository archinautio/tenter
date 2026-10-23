// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Rect

/**
 * A read-only view of the cells a [Screen.draw] call presented. A real terminal's reads its own
 * buffer, so it is only valid until the next draw; a [HeadlessTerminal]'s stays valid for good.
 */
public class CompletedFrame internal constructor(
    private val buffer: Buffer
) {

    public val width: Int get() = buffer.width
    public val height: Int get() = buffer.height

    /** The area that was drawn. */
    public val area: Rect get() = Rect(0, 0, width, height)

    /** @throws IndexOutOfBoundsException if ([x], [y]) is outside [area]. */
    public fun get(x: Int, y: Int): Cell = buffer.get(x, y)

    /** Each row's text, top to bottom. A wide character appears once, not once per column it covers. */
    public fun lines(): List<String> =
        (0 until height).map { y -> (0 until width).joinToString("") { x -> get(x, y).symbol } }

    internal companion object {
        /** A [width] x [height] frame of blank cells, as a freshly cleared screen shows. */
        internal fun blank(width: Int, height: Int): CompletedFrame = CompletedFrame(Buffer(width, height))
    }
}
