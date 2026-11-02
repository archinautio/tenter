// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import io.archinaut.tenter.terminal.Screen
import io.archinaut.tenter.terminal.Terminal
import io.archinaut.tenter.terminal.TerminalEvent
import tenterexample.colors.SchemeChoice
import tenterexample.shell.Showcase

/**
 * Runs the showcase in the process's terminal: `--scheme=<name>` picks its colors, `--hello` runs
 * the greeting instead, and `--headless` the noninteractive smoke check.
 */
public fun main(args: Array<String>) {
    when {
        "--hello" in args -> tenterexample.hello.main()
        "--headless" in args -> println(runHeadlessSmoke())
        else -> runShowcase(args)
    }
}

private fun runShowcase(args: Array<String>) {
    val terminal = Terminal()
    val choice = when (val result = SchemeChoice.from(args, terminal.colorSpace)) {
        is SchemeChoice.Result.Unknown -> {
            System.err.println(result.message)
            exitProcess(2)
        }
        is SchemeChoice.Result.Chosen -> {
            result.warning?.let(System.err::println)
            result.choice
        }
    }
    terminal.withScreen(choice.scheme.build()) { screen -> runInteractive(screen, Showcase(choice)) }
}

/** Draws [showcase] and redraws it after each event, and whenever its last frame said it would change. */
internal fun runInteractive(screen: Screen, showcase: Showcase) {
    runBlocking {
        // --8<-- [start:input-loop]
        val signals = Channel<LoopSignal>(Channel.UNLIMITED)
        // Each draw rearms the timer; a newer deadline cancels the pending one.
        val deadlines = Channel<Duration?>(Channel.CONFLATED)
        val reader = launch {
            screen.events().collect { signals.send(LoopSignal.Event(it)) }
            signals.close()
        }
        val timer = launch {
            deadlines.consumeAsFlow().collectLatest { wait ->
                if (wait != null) {
                    delay(wait)
                    signals.send(LoopSignal.Tick)
                }
            }
        }
        while (showcase.running) {
            screen.draw(showcase::render)
            deadlines.send(showcase.nextChangeIn)
            // A Resized event and a tick need no handling: the next draw follows the size and the time.
            val signal = signals.receiveCatching().getOrNull() ?: break
            if (signal is LoopSignal.Event && signal.event is TerminalEvent.Input) showcase.handle(signal.event.event)
        }
        // Cancelling the reader stops collecting the events, which releases the terminal.
        reader.cancel()
        timer.cancel()
        // --8<-- [end:input-loop]
    }
}
