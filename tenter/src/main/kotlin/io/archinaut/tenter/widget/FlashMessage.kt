// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Style
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A transient status-bar message: one row of [text], ellipsized to the available width. The
 * caller owns dismissal and removes it after [duration].
 */
public data class FlashMessage(
    val text: String,
    val duration: Duration = 3.seconds,
    val style: Style = Style(ChromeRole.INFO),
) : MeasurableWidget {

    override fun write(cursor: TextCursor) {
        cursor.writeLine(text, style)
    }
}
