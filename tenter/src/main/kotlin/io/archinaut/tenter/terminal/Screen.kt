// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Rect

import com.github.ajalt.mordant.input.MouseTracking
import kotlinx.coroutines.flow.Flow

/**
 * A [Terminal] while [Terminal.withScreen] runs: what an application draws frames to and reads
 * events from. It closes when that scope returns, and then refuses to draw or read.
 *
 * Each [draw] hands the caller a fresh [Frame] covering [area] and presents the result: a real
 * terminal sends only the cells that changed since the previous frame, a [HeadlessTerminal] keeps
 * it as its [HeadlessTerminal.frame].
 */
public class Screen internal constructor(
    internal val terminal: Terminal,
    private val session: Session,
) {

    private var open: Boolean = true

    /** The full drawable area of the next frame. It follows the terminal's size. */
    public val area: Rect
        get() {
            checkOpen()
            terminal.followSize()
            return terminal.area
        }

    /**
     * Calls [render] with a fresh [Frame] covering [area], then presents it. If [render] throws,
     * nothing is presented and the exception propagates.
     *
     * The returned [CompletedFrame] reads the drawn cells. A real terminal's stays valid until the
     * next draw; a [HeadlessTerminal]'s stays valid for good.
     *
     * @throws IllegalStateException once the [Terminal.withScreen] scope has returned.
     */
    public fun draw(render: (Frame) -> Unit): CompletedFrame {
        val area = area
        val buffer = session.nextBuffer(area.width, area.height)
        render(Frame(buffer))
        return session.present(buffer)
    }

    /**
     * This screen's events — user input with [mouseTracking], and size changes — as one cold flow,
     * in the manner of an application reading events in its own loop. The first
     * [TerminalEvent.Resized] fires when collection starts; later ones only when the size changes,
     * and the next [draw] follows the new size.
     *
     * On a real terminal, collecting acquires raw mode. To quit, stop collecting — for example with
     * `takeWhile` on the application's quit key: raw mode is released before collection returns, at
     * most one input poll interval (100 ms) later, whatever ends the collection. Only one collection
     * should be active at a time; collecting again later continues reading the same input.
     *
     * Terminal rendering, mutable panel/viewport state, and terminal-size observation should be
     * confined to one application execution context. The input reader runs on
     * [kotlinx.coroutines.Dispatchers.IO] and size polling in the collector's context; callers own
     * the dispatcher and confinement policy and should not infer that the merged flow runs on one
     * physical OS thread.
     *
     * A [HeadlessTerminal] ignores [mouseTracking] and reads its events from its script.
     *
     * @throws IllegalStateException once the [Terminal.withScreen] scope has returned.
     */
    public fun events(mouseTracking: MouseTracking = MouseTracking.Normal): Flow<TerminalEvent> {
        checkOpen()
        return terminal.events(mouseTracking)
    }

    /** Ends this screen; only [Terminal.closeScreen] calls it. */
    internal fun close() {
        open = false
        session.close()
    }

    private fun checkOpen() {
        check(open) { "the screen is closed; draw only inside terminal.withScreen { screen -> }" }
    }
}
