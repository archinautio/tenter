// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.input

/**
 * One context's bindings. [title] is its help section's heading, or null for a layer with no
 * section, whose bindings credit a [HintGroup] on another layer; [KeyMap] validates both.
 */
public data class KeyLayer<out A : Any>(
    public val title: String?,
    public val bindings: List<KeyBinding<A>>,
    public val hintGroups: List<HintGroup> = emptyList(),
)
