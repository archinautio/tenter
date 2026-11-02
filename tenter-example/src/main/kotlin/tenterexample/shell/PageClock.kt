// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.shell

import kotlin.time.Duration

/**
 * A page's time, derived from the showcase's: what the page sees as `now`. It stands still while
 * the page is hidden, so the page's timers resume where they were; it starts hidden.
 */
internal class PageClock {
    private var hiddenFor: Duration = Duration.ZERO
    private var hiddenSince: Duration? = Duration.ZERO

    internal fun now(showcase: Duration): Duration = (hiddenSince ?: showcase) - hiddenFor

    internal fun hide(showcase: Duration) {
        if (hiddenSince == null) hiddenSince = showcase
    }

    internal fun show(showcase: Duration) {
        val since = hiddenSince ?: return
        hiddenFor += showcase - since
        hiddenSince = null
    }
}
