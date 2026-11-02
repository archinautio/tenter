// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.color.ColorScheme
import io.archinaut.tenter.color.ColorSpace
import io.archinaut.tenter.screen.Rect

import com.github.ajalt.mordant.input.MouseTracking
import kotlinx.coroutines.flow.Flow
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

/**
 * A terminal an application draws to and reads input from, through the [Screen] that [withScreen]
 * opens: a real one from the [Terminal] function, or a [HeadlessTerminal] from [headless]. Creating
 * one writes nothing. A terminal is not thread-safe.
 */
public sealed class Terminal(
    width: Int,
    height: Int,
) {

    /** The color space this terminal shows, or `null` if it shows no color, so every color scheme draws without color. */
    public abstract val colorSpace: ColorSpace?

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

    /** Starts a scope drawing with [colorScheme]: enters the screen and returns what presents its frames. */
    internal abstract fun open(colorScheme: ColorScheme): Session

    /** Backs [Screen.events]. */
    internal abstract fun events(mouseTracking: MouseTracking): Flow<TerminalEvent>

    /**
     * Runs [block] with this terminal's [Screen], drawing with [colorScheme]. Entry clears the
     * screen; the screen closes and the terminal is restored when [block] returns or throws, and a
     * cleanup failure is suppressed onto an earlier one. [block] is inlined, so it may suspend.
     *
     * @throws IllegalStateException when a scope is already active on this terminal.
     */
    public inline fun <T> withScreen(colorScheme: ColorScheme = ColorScheme.Default, block: (Screen) -> T): T {
        // Kept minimal: this body is copied into callers, so the logic lives in the two calls.
        val screen = openScreen(colorScheme)
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
    internal fun openScreen(colorScheme: ColorScheme): Screen {
        check(!screenOpen) { "withScreen is already active on this terminal" }
        val session = open(colorScheme)
        screenOpen = true
        return Screen(this, session)
    }

    /** Closes [screen]; a cleanup failure is suppressed onto [failure] when there is one. */
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
        /** A [width] x [height] terminal that never touches a real one; its screen's events come from [script]. */
        public fun headless(
            width: Int,
            height: Int,
            script: suspend HeadlessInput.() -> Unit = {},
        ): HeadlessTerminal = HeadlessTerminal(width, height, script)
    }
}

/** The process's real terminal, drawing through [mordant]. */
public fun Terminal(mordant: MordantTerminal = MordantTerminal()): Terminal = AnsiTerminal(mordant)
