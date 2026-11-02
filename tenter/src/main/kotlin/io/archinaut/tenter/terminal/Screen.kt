// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Rect

import com.github.ajalt.mordant.input.MouseTracking
import kotlinx.coroutines.flow.Flow

/** A [Terminal] while [Terminal.withScreen] runs; once that scope returns, it refuses to draw or read. */
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
     * Calls [render] with a fresh [Frame] covering [area], then presents it; if [render] throws,
     * nothing is presented. A real terminal's [CompletedFrame] stays valid until the next draw, a
     * headless one's for good.
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
     * User input with [mouseTracking] and size changes as one cold flow, starting with a
     * [TerminalEvent.Resized]. On a real terminal, collecting holds raw mode, released within one
     * 100 ms input poll after collection ends. The input reader runs on
     * [kotlinx.coroutines.Dispatchers.IO]; size polling runs in the collector's context.
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
