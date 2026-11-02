// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.shell

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Point
import kotlin.time.Duration
import tenterexample.pages.PageView
import tenterexample.pages.ShowcasePage

/** A [page] with the state the showcase keeps for it and its [clock]; every time passed in is the showcase's. */
internal class PageEntry<S, A : Any>(internal val page: ShowcasePage<S, A>) {
    internal val clock: PageClock = PageClock()

    internal var state: S = page.initialState
        private set

    /** Handles [event] if the page binds it, returning whether it did. */
    internal fun press(event: KeyboardEvent, now: Duration): Boolean {
        val action = page.resolve(state, event) ?: return false
        state = page.handle(state, action, clock.now(now))
        return true
    }

    internal fun click(point: Point, now: Duration) {
        state = page.click(state, point, clock.now(now))
    }

    internal fun view(now: Duration): PageView = page.view(state, clock.now(now))

    internal fun hints(): List<KeySection> = page.hints(state)
}
