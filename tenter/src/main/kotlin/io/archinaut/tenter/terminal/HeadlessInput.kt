// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Size

import com.github.ajalt.mordant.input.InputEvent
import com.github.ajalt.mordant.input.KeyboardEvent

/**
 * What a [HeadlessTerminal]'s script can do: send input and resizes, and read the [frame] drawn in
 * response. It runs once, from the application's first collection of [Screen.events]. Each call
 * returns once the application asks for its next event; when the script returns, the flow
 * completes, and a script that throws fails the collection with its exception.
 */
public class HeadlessInput internal constructor(
    private val terminal: HeadlessTerminal,
) {

    /** The terminal's [HeadlessTerminal.frame]: what the application has drawn so far. */
    public val frame: CompletedFrame get() = terminal.frame

    /** Sends [event], as the user's terminal would report it. */
    public suspend fun send(event: InputEvent) {
        terminal.deliver(TerminalEvent.Input(event))
    }

    /** Sends a press of [key], with the given modifiers. */
    public suspend fun key(key: String, ctrl: Boolean = false, alt: Boolean = false, shift: Boolean = false) {
        send(KeyboardEvent(key, ctrl, alt, shift))
    }

    /** Resizes the terminal to [width] x [height], so the next draw uses it, and sends the [TerminalEvent.Resized]. */
    public suspend fun resize(width: Int, height: Int) {
        terminal.resize(width, height)
        terminal.deliver(TerminalEvent.Resized(Size(width, height)))
    }
}
