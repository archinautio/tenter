// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.KeyboardEvent
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.terminal.Terminal
import io.archinaut.tenter.terminal.TerminalEvent
import tenterexample.hello.runHello

internal class HelloExampleTest {
    // --8<-- [start:test]
    @Test
    fun `hello paints before reading and quits without consuming later input`() {
        val terminal = Terminal.headless(width = 50, height = 8)
        var read = 0
        val events = flow<TerminalEvent> {
            assertTrue(checkNotNull(terminal.lastFrame).lines().first().startsWith("Hello from Tenter"))
            read++
            emit(TerminalEvent.Input(KeyboardEvent("x")))
            read++
            emit(TerminalEvent.Input(KeyboardEvent("q")))
            error("input after quit must not be read")
        }

        runHello(terminal, events)

        assertEquals(2, read)
    }
    // --8<-- [end:test]
}
