// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import io.archinaut.tenter.widget.FlashMessage
import io.archinaut.tenter.widget.MeasurableWidget
import kotlin.time.Duration

/**
 * What a page shows at one moment, returned up to the shell: its [content], when it next changes
 * on its own, a [flash] for the status bar, and a [popup] to float over the content.
 */
internal data class PageView(
    val content: MeasurableWidget,
    val nextChangeIn: Duration? = null,
    val flash: FlashMessage? = null,
    val popup: Popup? = null,
)
