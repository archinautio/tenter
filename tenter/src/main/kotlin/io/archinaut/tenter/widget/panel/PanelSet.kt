// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect

/**
 * The state of one screen's panels, which the [Panels] widget renders: focus and cross-panel mode
 * transitions. [panels] is copied and must hold at least one panel, with distinct ids; focus
 * starts on the first. A panel belongs to one set for its lifetime, and a set that fails
 * validation claims none. Every id-addressed operation throws [IllegalArgumentException] for an
 * id the set does not declare.
 */
public class PanelSet<K : PanelId, I>(panels: List<Panel<K, I>>) {

    private val panels: List<Panel<K, I>> = panels.toList()

    init {
        require(this.panels.isNotEmpty()) { "A PanelSet needs at least one panel" }
        val ids = HashSet<K>()
        this.panels.forEach { panel ->
            require(ids.add(panel.id)) { "Panel id ${panel.id} appears more than once in this PanelSet" }
            panel.requireUnattached()
        }
        this.panels.forEach { it.claimAttachment() }
    }

    private var focusedId: K = this.panels.first().id

    /** The geometry of the last completed [Panels] frame, or null before the first one. */
    public var layout: PanelLayout<K>? = null
        private set

    /** The panel that receives focused input; a frame that renders no panel keeps it. */
    public val focused: K get() = focusedId

    private fun panelFor(id: K): Panel<K, I> =
        panels.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("Panel $id is not declared in this PanelSet")

    /** Focuses [id], demoting any other maximized panel. */
    public fun focus(id: K) {
        val panel = panelFor(id)
        panels.forEach { if (it !== panel) it.demoteFromMaximized() }
        focusedId = id
    }

    /** Cycles the focused panel's declared mode, demoting any other maximized panel. */
    public fun cycleFocusedMode(delta: Int) {
        val panel = panelFor(focusedId)
        val oldMode = panel.mode
        panel.cycleMode(delta)
        if (panel.mode == PanelMode.MAXIMIZED && oldMode != PanelMode.MAXIMIZED) {
            panels.forEach { if (it !== panel) it.demoteFromMaximized() }
        }
    }

    /** Queues scrolling panel [id] by ([dx], [dy]) cells. */
    public fun scroll(id: K, dx: Int, dy: Int) {
        panelFor(id).scrollBy(dx, dy)
    }

    /** Queues scrolling panel [id] by [direction] (-1 or +1) times its last viewport height, or one row without one. */
    public fun page(id: K, direction: Int) {
        val page = layout?.slotOf(id)?.content?.height?.coerceAtLeast(1) ?: 1
        panelFor(id).scrollBy(0, page * direction)
    }

    /** Queues recentering for this panel's next drawable frame, including while hidden. */
    public fun requestRecenter(id: K) {
        panelFor(id).requestRecenter()
    }

    /**
     * The panel at frame coordinate ([x], [y]) in the last completed frame, the last-painted one
     * where panels overlap, with the point in its content's own coordinates; a hit on its border or
     * padding has no content point.
     */
    public fun hitTest(x: Int, y: Int): PanelHit<K>? {
        val slot = layout?.panelAt(x, y) ?: return null
        val bounds = slot.contentBounds
        if (bounds == null || !slot.content.contains(x, y) || !bounds.contains(x, y)) return PanelHit(slot.id, null)
        return PanelHit(slot.id, Point(x - bounds.x, y - bounds.y))
    }

    /** The current mode of panel [id], for choosing its area in the next frame. */
    public fun modeOf(id: K): PanelMode = panelFor(id).mode

    /** Builds and paints the panels in [areas], in its order, then publishes [layout]. */
    internal fun render(canvas: Canvas, inputs: I, areas: Map<K, Rect>) {
        val rendered = areas.keys.map(::panelFor)
        normalizeFocus(areas.keys)

        val contents = rendered.associateBy({ it.id }, { it.content(inputs) })
        val origin = canvas.origin
        val slots = areas.map { (id, area) ->
            // Clipped to the canvas, so the published geometry is exactly what was painted.
            val local = area.offset(-origin.x, -origin.y).clipTo(canvas.area)
            val painted = panelFor(id).render(canvas.region(local), contents.getValue(id), focused = id == focusedId)
            painted.offset(local.x + origin.x, local.y + origin.y)
        }
        layout = PanelLayout(slots)
    }

    /** Moves focus onto the first rendered panel when the focused one is not rendered. */
    private fun normalizeFocus(rendered: Set<K>) {
        if (focusedId in rendered) return
        panels.firstOrNull { it.id in rendered }?.let { focus(it.id) }
    }
}
