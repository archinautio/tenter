// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import io.archinaut.tenter.terminal.TerminalEvent

/** What wakes the showcase's loop: a terminal event, or the time the last frame said it changes. */
internal sealed interface LoopSignal {
    data class Event(val event: TerminalEvent) : LoopSignal

    data object Tick : LoopSignal
}
