// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.color.ColorScheme
import io.archinaut.tenter.color.ColorSpace
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Size

import com.github.ajalt.mordant.input.MouseTracking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A [Terminal] that never touches a real one, created with [Terminal.headless]. It follows a real
 * terminal's rules, reports no [colorSpace], and ignores the color scheme, since frames hold roles.
 */
public class HeadlessTerminal internal constructor(
    width: Int,
    height: Int,
    script: suspend HeadlessInput.() -> Unit,
) : Terminal(width, height) {

    private val input: ScriptedInput = ScriptedInput { HeadlessInput(this).script() }

    /** What this terminal shows: the latest frame drawn, blank before the first and on entering [withScreen]. */
    public var frame: CompletedFrame = CompletedFrame.blank(area.width, area.height)
        private set

    override val colorSpace: ColorSpace? get() = null

    override fun open(colorScheme: ColorScheme): Session {
        frame = CompletedFrame.blank(area.width, area.height)
        return HeadlessSession()
    }

    override fun events(mouseTracking: MouseTracking): Flow<TerminalEvent> = flow {
        emit(TerminalEvent.Resized(Size(area.width, area.height)))
        input.emitTo(this)
    }

    /** Hands [event] to the application reading this terminal's events; see [HeadlessInput]. */
    internal suspend fun deliver(event: TerminalEvent) {
        input.send(event)
    }

    private inner class HeadlessSession : Session {

        // Every frame gets its own buffer, so a presented frame never changes afterwards.
        override fun nextBuffer(width: Int, height: Int): Buffer = Buffer(width, height)

        override fun present(buffer: Buffer): CompletedFrame = CompletedFrame(buffer).also { frame = it }

        override fun close() {}
    }
}
