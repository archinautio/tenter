// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.palette.DefaultRolePalette
import io.archinaut.tenter.palette.RolePalette

import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

// Mordant exposes cursor control but not the alternate screen buffer. These are raw escapes,
// gated by the same interactive check as Mordant's cursor implementation.
private const val ENTER_ALT_SCREEN: String = "\u001b[?1049h"
private const val EXIT_ALT_SCREEN: String = "\u001b[?1049l"

/**
 * Runs [block] with this terminal inside one alternate-screen and cursor scope, drawing with
 * [palette].
 *
 * Entry clears the screen, so the first [Terminal.draw] in [block] repaints fully. Entering again
 * after an earlier scope has returned is fine, also with another palette; a nested scope on the
 * same terminal throws [IllegalStateException].
 *
 * This operation does not collect input, run an event loop, or make the mutable terminal safe to
 * use from multiple threads. A caller may put a synchronous `runBlocking` application inside
 * [block] and read input from [Terminal.events], which acquires and releases raw mode separately
 * while it is collected.
 *
 * Entry and cleanup are exception-safe, including a failure after one of the entry operations has
 * already written to the terminal. Cleanup is idempotent. If the block or entry fails and cleanup
 * fails as well, the original failure is rethrown and the cleanup failure is suppressed on it.
 * Noninteractive terminals do not receive alternate-screen or cursor escape sequences.
 */
public fun <T> Terminal.withScreen(palette: RolePalette = DefaultRolePalette, block: (Terminal) -> T): T {
    // Outside the try: a rejected nested scope must not end the outer one.
    enterScope(palette)
    try {
        return runOnScreen(block)
    } finally {
        exitScope()
    }
}

private fun <T> Terminal.runOnScreen(block: (Terminal) -> T): T {
    val lifecycle = ScreenLifecycle(backend)
    var failure: Throwable? = null
    try {
        lifecycle.enter()
        clear()
        return block(this)
    } catch (thrown: Throwable) {
        failure = thrown
        throw thrown
    } finally {
        try {
            lifecycle.close()
        } catch (cleanupFailure: Throwable) {
            val originalFailure = failure
            if (originalFailure == null) {
                throw cleanupFailure
            }
            originalFailure.addSuppressed(cleanupFailure)
        }
    }
}

private class ScreenLifecycle(
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
