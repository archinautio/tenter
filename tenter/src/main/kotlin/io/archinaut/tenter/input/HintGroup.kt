// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.input

/**
 * One row of a help panel: a hand-written [label] for its keys, such as `"←→↑↓/wasd"`, and what they
 * do. [bindingless] marks a row for something outside the key map, such as the mouse wheel, which
 * is exempt from needing a binding.
 */
public data class HintGroup(
    val id: String,
    val label: String,
    val description: String,
    val bindingless: Boolean = false,
)
