// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.input

/**
 * One addressable set of bindings.
 *
 * [title] is the help panel's section heading, or null for a layer that never renders a section of
 * its own: its bindings are credited to a [HintGroup] declared on another layer, as a
 * precedence-only layer such as panel scrolling does.
 * Structural validity, including sectionless credits, is owned by [KeyMap] at construction.
 */
public data class KeyLayer<A : Any>(
    public val title: String?,
    public val bindings: List<KeyBinding<A>>,
    public val hintGroups: List<HintGroup> = emptyList(),
)
