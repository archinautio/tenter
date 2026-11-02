// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

// Mordant exposes cursor control but not the alternate screen buffer. These are raw escapes,
// gated by the same interactive check as Mordant's cursor implementation.
private const val ENTER_ALT_SCREEN: String = "\u001b[?1049h"
private const val EXIT_ALT_SCREEN: String = "\u001b[?1049l"

/** Hides the cursor and enters the alternate screen of a Mordant [terminal], and undoes both on [close]. */
internal class ScreenLifecycle(
    private val terminal: MordantTerminal,
) {
    private var cursorHidden: Boolean = false
    private var alternateScreenEntered: Boolean = false
    private var closed: Boolean = false

    internal fun enter() {
        check(!closed) { "screen lifecycle is already closed" }
        if (!terminal.terminalInfo.interactive) return

        // Set the flags before the writes. A sink can fail after partially accepting a write, so
        // cleanup must still attempt to undo every operation whose escape may have been sent.
        cursorHidden = true
        terminal.cursor.hide()
        alternateScreenEntered = true
        terminal.rawPrint(ENTER_ALT_SCREEN)
    }

    internal fun close() {
        if (closed) return
        closed = true

        var failure: Throwable? = null
        if (cursorHidden) {
            try {
                terminal.cursor.show()
            } catch (thrown: Throwable) {
                failure = thrown
            }
        }
        if (alternateScreenEntered) {
            try {
                terminal.rawPrint(EXIT_ALT_SCREEN)
            } catch (thrown: Throwable) {
                if (failure == null) {
                    failure = thrown
                } else {
                    failure.addSuppressed(thrown)
                }
            }
        }
        if (failure != null) throw failure
    }
}
