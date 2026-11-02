// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Point
import kotlin.time.Duration

/**
 * One page of the showcase, as pure functions of its own state [S]: the shell keeps that state
 * while other pages are shown, and passes the page's clock as `now`. Its keys resolve to [A].
 */
internal interface ShowcasePage<S, A : Any> {
    /** The page's name in the page list and on its panel. */
    val title: String

    val initialState: S

    /** The action [event] triggers on this page, or null to leave it to the shell. */
    fun resolve(state: S, event: KeyboardEvent): A?

    /** The help sections for the keys [resolve] currently binds. */
    fun hints(state: S): List<KeySection>

    fun handle(state: S, action: A, now: Duration): S

    /** [state] after a click at [point], in the page content's own coordinates. */
    fun click(state: S, point: Point, now: Duration): S = state

    fun view(state: S, now: Duration): PageView
}
