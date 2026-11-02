// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import java.util.Collections
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.ScrollState

/** Every rendered panel's geometry from one completed frame of [Panels], in paint order and frame coordinates. */
public class PanelLayout<K : PanelId> internal constructor(slots: List<Slot<K>>) {

    public val slots: List<Slot<K>> = Collections.unmodifiableList(ArrayList(slots))

    public data class Slot<K : PanelId>(
        public val id: K,
        /** The complete panel rectangle, including its border. */
        public val outer: Rect,
        /** The part the scrolling viewport showed, inside the border and padding. */
        public val content: Rect,
        /** The settled viewport observation, or null when this slot had no drawable frame. */
        public val scroll: ScrollState?,
        /** Where the content lay, extending past [content] where scrolled out of view, or null when this slot had no drawable frame. */
        public val contentBounds: Rect?,
    ) {
        internal fun offset(dx: Int, dy: Int): Slot<K> = copy(
            outer = outer.offset(dx, dy),
            content = content.offset(dx, dy),
            contentBounds = contentBounds?.offset(dx, dy),
        )
    }

    /** The slot for [id], or null when that panel was not rendered in this frame. */
    public fun slotOf(id: K): Slot<K>? = slots.firstOrNull { it.id == id }

    /** The last-painted panel whose outer rectangle contains ([x], [y]), or null. */
    internal fun panelAt(x: Int, y: Int): Slot<K>? = slots.lastOrNull { it.outer.contains(x, y) }
}
