// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/** Adds two composition dimensions, failing with an error naming [description] instead of overflowing. */
internal fun checkedAdd(left: Int, right: Int, description: String): Int =
    try {
        Math.addExact(left, right)
    } catch (_: ArithmeticException) {
        error("$description overflowed")
    }
