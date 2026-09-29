// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.input

/** A titled group of [hints] — e.g. one "local" section (current context) or a "global" section. */
public data class KeySection(
    val title: String,
    val hints: List<KeyHint>
)
