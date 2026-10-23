// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Size

import com.github.ajalt.mordant.input.InputEvent
import com.github.ajalt.mordant.input.KeyboardEvent

/**
 * What a [HeadlessTerminal]'s script does: the input a user would type, and the resizes they would
 * make, while it checks the [frame] the application drew in response.
 *
 * The script runs once per terminal, the first time the application collects [Screen.events], and
 * carries on across later collections, as a real user's input does. Each call hands its event to the
 * application and returns once the application asks for its next event — so by then it has handled
 * the event and drawn whatever it draws in response. That holds when the application collects the
 * flow directly (through operators such as `map`, `onEach` or `takeWhile`); an operator that buffers
 * in between, such as `merge` or `buffer`, lets a call return as soon as the event is queued.
 *
 * When the script returns, the events flow completes. If the application stops collecting for good
 * (on its quit key, say), the rest of the script never runs, so check the final frame after the
 * application returns. A script that throws fails the application's collection with the same
 * exception.
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
