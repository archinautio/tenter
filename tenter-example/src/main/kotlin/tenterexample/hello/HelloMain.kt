// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.hello

import com.github.ajalt.mordant.input.KeyboardEvent
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import io.archinaut.tenter.terminal.Terminal
import io.archinaut.tenter.terminal.TerminalEvent
import io.archinaut.tenter.widget.Paragraph

public fun main() {
    runHello()
}

/** The application owns the loop; tests pass a headless terminal that scripts its input. */
public fun runHello(terminal: Terminal = Terminal()) {
    val greeting = Paragraph("Hello from Tenter — press q to quit")
    terminal.withScreen { screen ->
        screen.draw { frame -> frame.renderWidget(greeting, frame.area) }
        runBlocking {
            screen.events().takeWhile { event -> !event.isKey("q") }.collect { }
        }
    }
}

private fun TerminalEvent.isKey(key: String): Boolean =
    this is TerminalEvent.Input && (event as? KeyboardEvent)?.key == key
