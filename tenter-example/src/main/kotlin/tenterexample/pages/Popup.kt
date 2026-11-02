// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.widget.Widget

/** A [content] of [size] cells that the shell centers over the page, clearing what lies beneath. */
internal data class Popup(
    val size: Size,
    val content: Widget,
)
