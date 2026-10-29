// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.mordant.input.InputEvent
import io.archinaut.tenter.screen.Size

/** A terminal-level event: input from the user, or a new terminal size. */
public sealed interface TerminalEvent {
    /** A Mordant keyboard or mouse event, as the terminal reported it. */
    public data class Input(val event: InputEvent) : TerminalEvent

    /** The terminal now has [size] cells. */
    public data class Resized(val size: Size) : TerminalEvent
}
