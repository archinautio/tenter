// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/**
 * Adds two composition dimensions, failing with a named error instead of wrapping into a negative
 * size. Every composite ([Row], [Flow], [block], [RowWriter])
 * derives its dimensions this way, so an overflow reports which composition produced it rather
 * than surfacing later as a `ContentLayout` size assertion.
 */
internal fun checkedAdd(left: Int, right: Int, description: String): Int =
    try {
        Math.addExact(left, right)
    } catch (_: ArithmeticException) {
        error("$description overflowed")
    }
