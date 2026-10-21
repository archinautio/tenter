// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import java.util.Collections
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.ScrollState

/**
 * Every rendered panel's immutable geometry from one completed frame of [Panels], in paint order.
 * Coordinates are frame coordinates, the same space as terminal mouse events.
 */
public class PanelLayout<K : PanelId> internal constructor(slots: List<Slot<K>>) {

    public val slots: List<Slot<K>> = Collections.unmodifiableList(ArrayList(slots))

    /** One panel's identity and settled placement from this frame. */
    public data class Slot<K : PanelId>(
        public val id: K,
        /** The complete panel rectangle, including its border. */
        public val outer: Rect,
        /** The rectangle painted by the scrolling viewport inside the border and padding. */
        public val content: Rect,
        /** The settled viewport observation, or null when this slot had no drawable frame. */
        public val scroll: ScrollState?,
    )

    /** The slot for [id], or null when that panel was not rendered in this frame. */
    public fun slotOf(id: K): Slot<K>? = slots.firstOrNull { it.id == id }

    /** The last-painted panel whose outer rectangle contains ([x], [y]), or null. */
    internal fun panelAt(x: Int, y: Int): Slot<K>? = slots.lastOrNull { it.outer.contains(x, y) }

    internal companion object {
        /** A slot for a panel painted into [outer], before its viewport has settled. */
        fun <K : PanelId> slot(id: K, outer: Rect): Slot<K> =
            Slot(id = id, outer = outer, content = outer.inner(Block.VIEWPORT_INSET), scroll = null)
    }
}
