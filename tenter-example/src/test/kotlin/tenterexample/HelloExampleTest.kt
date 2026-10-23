// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.terminal.Terminal
import tenterexample.hello.runHello

internal class HelloExampleTest {
    // --8<-- [start:test]
    @Test
    fun `hello paints before reading and stops reading at q`() {
        var keysRead = 0
        val terminal = Terminal.headless(width = 50, height = 8) {
            assertTrue(frame.lines().first().startsWith("Hello from Tenter"))
            key("x")
            keysRead++
            key("q")
            keysRead++ // never runs: hello stops reading at q
        }

        runHello(terminal)

        assertEquals(1, keysRead)
    }
    // --8<-- [end:test]
}
