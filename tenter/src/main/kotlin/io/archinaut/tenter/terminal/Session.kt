// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer

/**
 * One [Terminal.withScreen] scope as its terminal sees it: where frames are drawn and presented.
 * Each kind of [Terminal] opens its own.
 */
internal interface Session {

    /** A blank [width] x [height] buffer to draw the next frame into. */
    fun nextBuffer(width: Int, height: Int): Buffer

    /** Shows the finished [buffer], which this session now owns, and returns it as a completed frame. */
    fun present(buffer: Buffer): CompletedFrame

    /** Ends the scope, restoring whatever opening it changed. */
    fun close()
}
