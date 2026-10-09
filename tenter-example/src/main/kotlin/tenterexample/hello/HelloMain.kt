// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.hello

import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import io.archinaut.tenter.palette.DefaultRolePalette
import io.archinaut.tenter.terminal.TerminalEvent
import io.archinaut.tenter.terminal.inputEvents
import io.archinaut.tenter.terminal.withScreen
import io.archinaut.tenter.widget.contentWidget

public fun main() {
    val backend = Terminal()
    runHello(backend, backend.inputEvents(MouseTracking.Normal) { it.key == "q" })
}

/** The application owns the loop; tests can supply input without acquiring a real terminal. */
public fun runHello(backend: Terminal, events: Flow<TerminalEvent>) {
    val greeting = contentWidget { cursor -> cursor.writeLine("Hello from Tenter — press q to quit") }
    backend.withScreen(DefaultRolePalette) { terminal ->
        terminal.draw { frame -> frame.renderWidget(greeting, frame.area) }
        runBlocking {
            events.takeWhile { it !is TerminalEvent.Quit }.collect { }
        }
    }
}
