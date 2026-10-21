// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.ViewportState

/**
 * The state of one screen's stateful panels, which the [Panels] widget renders. It owns focus and
 * cross-panel transitions; each [Panel] retains its own mode and [ViewportState]. Where each panel
 * goes is the application's decision, made every frame by the areas it gives [Panels].
 *
 * [panels] is copied, and must hold at least one panel with distinct ids. Focus starts on the
 * first. A panel instance is claimed by exactly one set for its lifetime; a set that fails
 * validation claims none.
 */
public class PanelSet<K : PanelId, I>(panels: List<Panel<K, I>>) {

    /** Panel declarations in declaration order, copied so later caller mutation cannot change them. */
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

    /**
     * The geometry and settled scroll of the last completed [Panels] frame, in frame coordinates,
     * or null before the first one. A failed frame leaves the previous layout in place.
     */
    public var layout: PanelLayout<K>? = null
        private set

    /**
     * The panel that receives focused input. Never absent: a set always owns at least one panel,
     * and a frame with nothing rendered retains the focus it had rather than dropping it, so focus
     * returns where the user left it once panels are rendered again.
     */
    public val focused: K get() = focusedId

    /**
     * The declared panel named by [id].
     *
     * @throws IllegalArgumentException if [id] does not name a panel in this set. Every id-addressed
     * operation validates this way: the panel list is fixed at construction, so an id from outside
     * it is a caller mistake, never a state to observe.
     */
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

    /** Scrolls panel [id] by ([dx], [dy]) cells; pass [focused] for the panel receiving input. */
    public fun scroll(id: K, dx: Int, dy: Int) {
        panelFor(id).scrollBy(dx, dy)
    }

    /**
     * Scrolls panel [id] by one viewport height of its last completed frame; [direction] is -1 or
     * +1. Without a displayed viewport, a page is one row.
     */
    public fun page(id: K, direction: Int) {
        val page = layout?.slotOf(id)?.content?.height?.coerceAtLeast(1) ?: 1
        panelFor(id).scrollBy(0, page * direction)
    }

    /** Queues recentering for this panel's next drawable frame, including while hidden. */
    public fun requestRecenter(id: K) {
        panelFor(id).requestRecenter()
    }

    /**
     * Resolves frame coordinate ([x], [y]) against the last completed frame; where panels overlap,
     * the last-painted one wins. Border and padding hits identify the panel but have no content
     * point; a content point is translated through the settled offset and the scrolling panel's
     * reclaimable top spacer.
     */
    public fun hitTest(x: Int, y: Int): PanelHit<K>? {
        val slot = layout?.panelAt(x, y) ?: return null
        val scroll = slot.scroll ?: return PanelHit(slot.id, null)
        if (!slot.content.contains(x, y)) return PanelHit(slot.id, null)

        val contentX = x - slot.content.x + scroll.offset.x
        val contentY = y - slot.content.y + scroll.offset.y - Block.PADDING.verticalEdges().top
        val contentHeight = scroll.content.height - Block.PADDING.verticalEdges().top
        if (contentX !in 0 until scroll.content.width || contentY !in 0 until contentHeight) {
            return PanelHit(slot.id, null)
        }
        return PanelHit(slot.id, Point(contentX, contentY))
    }

    /**
     * The mode observed for [id] — an immutable observation of managed state; callers cannot
     * mutate a panel through it. Read it to choose each panel's area for the next frame.
     */
    public fun modeOf(id: K): PanelMode = panelFor(id).mode

    /**
     * The body of [Panels.render]: builds each rendered panel's content exactly once, paints every
     * panel into its area in [areas]' order, then publishes [layout]. [areas] are frame coordinates.
     */
    internal fun render(canvas: Canvas, inputs: I, areas: Map<K, Rect>) {
        val rendered = areas.keys.map(::panelFor)
        normalizeFocus(areas.keys)

        val contents = rendered.associateBy({ it.id }, { it.content(inputs) })
        val origin = canvas.origin
        val slots = areas.map { (id, area) ->
            // Clipped to the canvas, so the published geometry is exactly what was painted.
            val local = area.offset(-origin.x, -origin.y).clipTo(canvas.area)
            PanelLayout.slot(id, local)
        }
        val candidates = slots.associate { slot ->
            slot.id to panelFor(slot.id).render(
                canvas.region(slot.outer),
                contents.getValue(slot.id),
                focused = slot.id == focusedId,
            )
        }
        candidates.forEach { (id, candidate) -> panelFor(id).settle(candidate) }
        layout = PanelLayout(
            slots.map { slot ->
                val drawable = !slot.content.isEmpty
                slot.copy(
                    outer = slot.outer.offset(origin.x, origin.y),
                    content = slot.content.offset(origin.x, origin.y),
                    scroll = if (drawable) candidates.getValue(slot.id).settled else null,
                )
            },
        )
    }

    /**
     * Moves focus onto the first rendered panel, in declaration order, when the focused one is
     * not rendered. A frame with nothing rendered keeps its current focus: there is no better
     * panel to move to, and dropping it would silently relocate the user's focus once panels
     * are rendered again.
     */
    private fun normalizeFocus(rendered: Set<K>) {
        if (focusedId in rendered) return
        panels.firstOrNull { it.id in rendered }?.let { focus(it.id) }
    }
}
