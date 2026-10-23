// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

/**
 * Runs a [script] of input once, handing each event it [send]s to whichever collection is reading
 * at the time, in step with it: [send] returns only when that collection asks for its next event.
 *
 * The script runs on [Dispatchers.Unconfined], so it starts in the first collection's thread and
 * each step resumes in the thread that asks for the next event — the application's own — and never
 * races it.
 */
internal class ScriptedInput(
    private val script: suspend () -> Unit,
) {

    private class Step(val event: TerminalEvent) {
        val handled: CompletableDeferred<Unit> = CompletableDeferred()
    }

    // Rendezvous: the script waits at each step until a collection takes it.
    private val steps: Channel<Step> = Channel()
    private var started: Boolean = false

    // The step the latest collection emitted; it is handled once a collection asks for another.
    private var pending: Step? = null

    /** Emits the script's events to [collector] until the script returns or fails. */
    suspend fun emitTo(collector: FlowCollector<TerminalEvent>) {
        start()
        while (true) {
            pending?.handled?.complete(Unit)
            pending = null
            val result = steps.receiveCatching()
            val step = result.getOrNull()
            if (step == null) {
                result.exceptionOrNull()?.let { throw it }
                return
            }
            pending = step
            collector.emit(step.event)
        }
    }

    /** Hands [event] to the reading collection and waits until it asks for the next one. */
    suspend fun send(event: TerminalEvent) {
        val step = Step(event)
        steps.send(step)
        step.handled.await()
    }

    private fun start() {
        if (started) return
        started = true
        CoroutineScope(Dispatchers.Unconfined).launch {
            try {
                script()
                steps.close()
            } catch (failure: Throwable) {
                steps.close(failure)
            }
        }
    }
}
