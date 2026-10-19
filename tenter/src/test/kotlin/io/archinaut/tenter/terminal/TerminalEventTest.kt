// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Size
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.terminal.TerminalInterface
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertFalse
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class TerminalEventTest {

    /** A recorder that also reports whether raw mode is active, which Mordant keeps private. */
    private class RawModeProbe(
        private val recorder: TerminalRecorder,
    ) : TerminalInterface by recorder {
        var rawModeActive: Boolean = false
            private set
        var rawModeEntries: Int = 0
            private set

        override fun enterRawMode(mouseTracking: MouseTracking): AutoCloseable {
            val inner = recorder.enterRawMode(mouseTracking)
            rawModeActive = true
            rawModeEntries++
            return AutoCloseable {
                inner.close()
                rawModeActive = false
            }
        }
    }

    @Nested
    inner class EventsTest {

        @Test
        fun `stopping collection on the application's quit key releases raw mode before returning`() {
            val keys = listOf(KeyboardEvent("a"), KeyboardEvent("q"), KeyboardEvent("b"))
            val probe = RawModeProbe(TerminalRecorder(width = 80, height = 20).apply { inputEvents = keys.toMutableList() })
            val terminal = Terminal(terminalInterface = probe)

            val seen = runBlocking {
                withTimeout(5.seconds) {
                    terminal.events()
                        .takeWhile { !(it is TerminalEvent.Input && (it.event as? KeyboardEvent)?.key == "q") }
                        .filterIsInstance<TerminalEvent.Input>()
                        .toList()
                }
            }

            assertEquals(listOf(TerminalEvent.Input(KeyboardEvent("a"))), seen)
            assertEquals(1, probe.rawModeEntries)
            assertFalse(probe.rawModeActive)
        }
    }

    @Nested
    inner class ResizeEventsTest {

        /**
         * Mordant's `Size` has no `equals` and `updateSize()` returns a fresh instance per call,
         * so deduplicating on `Size` itself silently does nothing: every poll emits and a naive
         * collector re-renders several times a second forever. Beyond the waste, anything keyed
         * off "the terminal resized" then fires on a timer and stomps the user's manual scroll a
         * fraction of a second after they make it.
         */
        @Test
        fun `an unchanging terminal size emits exactly one resize event, not one per poll`() = runTest {
            val terminal = Terminal(terminalInterface = TerminalRecorder(width = 80, height = 20))

            val seen = mutableListOf<TerminalEvent>()
            val job = launch { terminal.resizeEvents(period = 10.milliseconds).toList(seen) }
            advanceTimeBy(500.milliseconds) // ~50 poll periods
            job.cancel()

            assertEquals(1, seen.size, "expected a single startup emission, got: $seen")
            assertEquals(TerminalEvent.Resized(Size(80, 20)), seen.single())
        }

        @Test
        fun `resize polling rejects nonpositive and infinite periods`() {
            val terminal = Terminal(terminalInterface = TerminalRecorder(width = 80, height = 20))

            assertThrows<IllegalArgumentException> { terminal.resizeEvents(Duration.ZERO) }
            assertThrows<IllegalArgumentException> { terminal.resizeEvents((-1).milliseconds) }
            assertThrows<IllegalArgumentException> { terminal.resizeEvents(Duration.INFINITE) }
        }
    }
}
