// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.terminal.TerminalEvent
import tenterexample.hello.runHello

internal class HelloExampleTest {
    @Test
    fun `hello paints before reading and quits without consuming later input`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.NONE, width = 50, height = 8)
        val terminal = Terminal(ansiLevel = AnsiLevel.NONE, terminalInterface = recorder)
        var read = 0
        val events = flow<TerminalEvent> {
            assertTrue(recorder.output().contains("Hello from Tenter"))
            read++
            emit(TerminalEvent.Input(KeyboardEvent("x")))
            read++
            emit(TerminalEvent.Input(KeyboardEvent("q")))
            error("input after quit must not be read")
        }

        runHello(terminal) { events }

        assertEquals(2, read)
    }
}
