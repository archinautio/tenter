// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.hello

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import io.archinaut.tenter.palette.DefaultRolePalette
import io.archinaut.tenter.terminal.TerminalEvent
import io.archinaut.tenter.terminal.events
import io.archinaut.tenter.terminal.withScreen
import io.archinaut.tenter.widget.Paragraph

public fun main() {
    val backend = Terminal()
    runHello(backend, backend.events())
}

/** The application owns the loop; tests can supply input without acquiring a real terminal. */
public fun runHello(backend: Terminal, events: Flow<TerminalEvent>) {
    val greeting = Paragraph("Hello from Tenter — press q to quit")
    backend.withScreen(DefaultRolePalette) { terminal ->
        terminal.draw { frame -> frame.renderWidget(greeting, frame.area) }
        runBlocking {
            events.takeWhile { event -> !event.isKey("q") }.collect { }
        }
    }
}

private fun TerminalEvent.isKey(key: String): Boolean =
    this is TerminalEvent.Input && (event as? KeyboardEvent)?.key == key
