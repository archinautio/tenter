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

/**
 * This terminal's events — user input with [mouseTracking], and size changes — as one cold flow,
 * in the manner of an application reading events in its own loop. The first [TerminalEvent.Resized]
 * fires at startup; later ones only when the size changes.
 *
 * Collecting acquires raw mode. To quit, stop collecting — for example with `takeWhile` on the
 * application's quit key: raw mode is released before collection returns, at most one input poll
 * interval (100 ms) later, whatever ends the collection. Only one collection should be active for a
 * terminal at a time, so compose this flow once in the application's event loop.
 *
 * Terminal rendering, mutable panel/viewport state, and terminal-size observation should be
 * confined to one application execution context. The input reader runs on [Dispatchers.IO] and
 * size polling in the collector's context; callers own the dispatcher and confinement policy and
 * should not infer that the merged flow runs on one physical OS thread.
 */
public fun MordantTerminal.events(mouseTracking: MouseTracking = MouseTracking.Normal): Flow<TerminalEvent> =
    merge(inputEvents(mouseTracking), resizeEvents())

/**
 * Input events read in raw mode on [Dispatchers.IO].
 *
 * Mordant has no raw-mode shutdown hook for cancellation. A naive infinite-blocking `readEvent()`
 * only observes cancellation at its next `emit`, which may never come if the user stops typing —
 * leaving the terminal stuck in raw mode and the JVM unable to exit. [rawModePollingFlow] polls with
 * a short timeout and checks [isActive] between polls, so any cancellation, from any cause —
 * including the application simply stopping collection — is noticed within the poll timeout.
 */
internal fun MordantTerminal.inputEvents(mouseTracking: MouseTracking): Flow<TerminalEvent> =
    rawModePollingFlow(mouseTracking)
        .map<InputEvent, TerminalEvent> { TerminalEvent.Input(it) }
        .flowOn(Dispatchers.IO)

/**
 * Enters raw mode and emits input events, polling with [pollTimeout] instead of blocking
 * indefinitely so that coroutine cancellation is observed promptly even while idle. See
 * [inputEvents] for why this matters.
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
 * A cold [TerminalEvent.Resized] flow polling [MordantTerminal.updateSize] every [period], which
 * must be finite and strictly positive. The first emission fires once at startup; later ones only
 * when the size actually changes.
 *
 * ### Mordant 3.0.2: `Size` has no `equals`
 * [com.github.ajalt.mordant.rendering.Size] is a plain class, not a data class, so it inherits
 * identity equality — and [MordantTerminal.updateSize] hands back a **fresh instance every call**.
 * Deduplicating on it would therefore never suppress anything: every poll would emit, and a
 * collector would re-render several times a second forever, indistinguishable from a real resize.
 * Each poll is therefore converted to Tenter's [Size], which compares by value, before
 * deduplicating.
 *
 * This flow intentionally has **no** [flowOn] — it runs in the collector's context so that size
 * reads can be co-located with rendering without cross-thread synchronisation.
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
