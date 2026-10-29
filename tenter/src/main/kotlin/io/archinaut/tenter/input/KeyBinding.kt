// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.input

import com.github.ajalt.mordant.input.KeyboardEvent

/**
 * One [chord] bound to one action, credited to the [HintGroup] that documents it. The chord matches
 * an event exactly as the terminal reports it, modifiers included, with no folding.
 */
public data class KeyBinding<out A : Any>(
    val chord: KeyboardEvent,
    val action: A,
    val hintGroup: String,
) {
    init {
        require(chord.key.isNotEmpty()) { "A key binding needs a key" }
    }
}
