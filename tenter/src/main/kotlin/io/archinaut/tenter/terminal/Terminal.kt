// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.palette.DefaultRolePalette
import io.archinaut.tenter.palette.RolePalette
import io.archinaut.tenter.screen.Rect

import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.rendering.AnsiLevel
import kotlinx.coroutines.flow.Flow
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

/**
 * A terminal an application draws to and reads input from, through the [Screen] that [withScreen]
 * opens.
 *
 * There are two kinds. [Terminal] (the factory function) is a real one over Mordant;
 * [Terminal.headless] is a [HeadlessTerminal] that never touches one, for tests and headless
 * rendering. Both follow the same rules: creating one writes nothing, and only a [Screen] draws.
 *
 * Picking *which* palette to pass to [withScreen] (a light/dark choice, [ansiLevel], letting the
 * user override it) is the host application's job. A terminal is not thread-safe.
 */
public sealed class Terminal(
    width: Int,
    height: Int,
) {

    /** The color level this terminal supports, for choosing the palette to pass to [withScreen]. */
    public abstract val ansiLevel: AnsiLevel

    private var width: Int = width.coerceAtLeast(0)
    private var height: Int = height.coerceAtLeast(0)
    private var screenOpen: Boolean = false

    /** The full drawable area of the next frame. */
    internal val area: Rect get() = Rect(0, 0, width, height)

    /**
     * Resizes the drawing area to [width] x [height]. A real terminal keeps it until Mordant reports
     * a new size.
     */
    internal fun resize(width: Int, height: Int) {
        require(width >= 0) { "terminal width must not be negative: $width" }
        require(height >= 0) { "terminal height must not be negative: $height" }
        this.width = width
        this.height = height
    }

    /** Brings [area] up to date before a draw. */
    internal open fun followSize() {}

    /** Starts a scope drawing with [palette]: enters the screen and returns what presents its frames. */
    internal abstract fun open(palette: RolePalette): Session

    /** Backs [Screen.events]. */
    internal abstract fun events(mouseTracking: MouseTracking): Flow<TerminalEvent>

    /**
     * Runs [block] with this terminal's [Screen], drawing with [palette], whose [ColorRole]s resolve
     * every color the application draws with.
     *
     * Entry clears the screen, so the first [Screen.draw] repaints fully. Entering again after an
     * earlier scope has returned is fine, also with another palette; a nested scope on the same
     * terminal throws [IllegalStateException]. The [Screen] closes when [block] returns.
     *
     * This operation does not collect input, run an event loop, or make the terminal safe to use from
     * multiple threads. [block] is inlined, so inside a coroutine it may suspend — for example to run
     * the application's event loop over [Screen.events] — and a synchronous caller may put a
     * `runBlocking` application inside it instead. Either way, keep every use of the screen confined
     * to one execution context.
     *
     * Entry and cleanup are exception-safe, including a failure after entry has already written to
     * the terminal. If the block or entry fails and cleanup fails as well, the original failure is
     * rethrown and the cleanup failure is suppressed on it.
     */
    public inline fun <T> withScreen(palette: RolePalette = DefaultRolePalette, block: (Screen) -> T): T {
        // Kept minimal: this body is copied into callers, so the logic lives in the two calls.
        val screen = openScreen(palette)
        var failure: Throwable? = null
        try {
            return block(screen)
        } catch (thrown: Throwable) {
            failure = thrown
            throw thrown
        } finally {
            closeScreen(screen, failure)
        }
    }

    /** Opens the [Screen] that [withScreen] runs its block with. */
    @PublishedApi
    internal fun openScreen(palette: RolePalette): Screen {
        check(!screenOpen) { "withScreen is already active on this terminal" }
        val session = open(palette)
        screenOpen = true
        return Screen(this, session)
    }

    /**
     * Closes a [screen] that [openScreen] returned, even if closing its session fails. A cleanup
     * failure is suppressed on [failure], the block's or entry's own, when there is one.
     */
    @PublishedApi
    internal fun closeScreen(screen: Screen, failure: Throwable?) {
        try {
            screen.close()
        } catch (cleanupFailure: Throwable) {
            if (failure == null) throw cleanupFailure
            failure.addSuppressed(cleanupFailure)
        } finally {
            screenOpen = false
        }
    }

    public companion object {
        /**
         * A [width] x [height] terminal that never touches a real one, for tests and headless
         * rendering. Its screen's events come from [script], which runs once, in step with the
         * application reading them; see [HeadlessInput].
         */
        public fun headless(
            width: Int,
            height: Int,
            script: suspend HeadlessInput.() -> Unit = {},
        ): HeadlessTerminal = HeadlessTerminal(width, height, script)
    }
}

/**
 * The process's real terminal, drawing through [mordant]. Pass a configured Mordant terminal to
 * choose its color level or size yourself.
 */
public fun Terminal(mordant: MordantTerminal = MordantTerminal()): Terminal = AnsiTerminal(mordant)
