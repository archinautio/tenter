// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.mordant.input.InputEvent
import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.input.enterRawMode
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal
import io.archinaut.tenter.screen.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** A terminal-level event: input from the user, or a new terminal size. */
public sealed interface TerminalEvent {
    /** A Mordant keyboard or mouse event, as the terminal reported it. */
    public data class Input(val event: InputEvent) : TerminalEvent

    /** The terminal now has [size] cells. */
    public data class Resized(val size: Size) : TerminalEvent
}

/** Backs [Screen.events] on a real terminal: input and resize events merged into one cold flow. */
internal fun MordantTerminal.events(mouseTracking: MouseTracking): Flow<TerminalEvent> =
    merge(inputEvents(mouseTracking), resizeEvents())

/** Input events read in raw mode on [Dispatchers.IO]. */
internal fun MordantTerminal.inputEvents(mouseTracking: MouseTracking): Flow<TerminalEvent> =
    rawModePollingFlow(mouseTracking)
        .map<InputEvent, TerminalEvent> { TerminalEvent.Input(it) }
        .flowOn(Dispatchers.IO)

/**
 * Enters raw mode and emits input events, polling with [pollTimeout]. A blocking read would notice
 * cancellation only at its next event, which may never come, leaving the terminal in raw mode.
 */
private fun MordantTerminal.rawModePollingFlow(
    mouseTracking: MouseTracking,
    pollTimeout: Duration = 100.milliseconds,
): Flow<InputEvent> = flow {
    enterRawMode(mouseTracking).use { rawMode ->
        while (currentCoroutineContext().isActive) {
            val event = rawMode.readEventOrNull(pollTimeout) ?: continue
            emit(event)
        }
    }
}

/**
 * [TerminalEvent.Resized] once at startup and then on each size change, polled every positive
 * [period]. Polls are compared as Tenter [Size]s because Mordant's `Size` compares by identity and
 * `updateSize` returns a fresh one each time. It runs in the collector's context, beside rendering.
 */
internal fun MordantTerminal.resizeEvents(period: Duration = 200.milliseconds): Flow<TerminalEvent> {
    require(period.isFinite() && period > Duration.ZERO) {
        "resize polling period must be finite and positive: $period"
    }
    return flow { while (true) { emit(updateSize()); delay(period) } }
        .map { Size(it.width, it.height) }
        .distinctUntilChanged()
        .map { TerminalEvent.Resized(it) }
}
