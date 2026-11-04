// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.VerticalAlign

/**
 * Where [content] cells start within [available] cells. [TextAlign.NONE] and [TextAlign.JUSTIFY]
 * start flush left; justifying is the text's own concern. Content that does not fit starts at 0.
 */
internal fun TextAlign.offset(content: Int, available: Int): Int {
    val slack = (available - content).coerceAtLeast(0)
    return when (this) {
        TextAlign.CENTER -> slack / 2
        TextAlign.RIGHT -> slack
        TextAlign.LEFT, TextAlign.NONE, TextAlign.JUSTIFY -> 0
    }
}

/** Where [content] cells start within [available] cells. Content that does not fit starts at 0. */
internal fun VerticalAlign.offset(content: Int, available: Int): Int {
    val slack = (available - content).coerceAtLeast(0)
    return when (this) {
        VerticalAlign.TOP -> 0
        VerticalAlign.MIDDLE -> slack / 2
        VerticalAlign.BOTTOM -> slack
    }
}
