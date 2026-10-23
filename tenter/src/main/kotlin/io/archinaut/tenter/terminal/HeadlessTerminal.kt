// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.palette.RolePalette
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Size

import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.rendering.AnsiLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A [Terminal] that never touches a real one, for tests and headless rendering; create it with
 * [Terminal.headless].
 *
 * It follows a real terminal's rules: only a [Screen] draws, and nested scopes throw. It reports
 * [AnsiLevel.NONE] and ignores the palette, since frames hold color roles, not colors. Its screen's
 * events come from the script it was created with; see [HeadlessInput].
 */
public class HeadlessTerminal internal constructor(
    width: Int,
    height: Int,
    script: suspend HeadlessInput.() -> Unit,
) : Terminal(width, height) {

    private val input: ScriptedInput = ScriptedInput { HeadlessInput(this).script() }

    /**
     * What this terminal shows: the latest frame a [Screen] drew, or blank cells before the first
     * one. Entering [withScreen] blanks it, as a real terminal clears; it keeps the last frame after
     * the scope returns, so a test reads it once the application has finished.
     */
    public var frame: CompletedFrame = CompletedFrame.blank(area.width, area.height)
        private set

    override val ansiLevel: AnsiLevel get() = AnsiLevel.NONE

    override fun open(palette: RolePalette): Session {
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
