// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.ViewportState

/**
 * The state of one screen's stateful panels, rendered by the [Panels] widget in the manner of a
 * Ratatui `StatefulWidget` state. It owns focus and cross-panel transitions; each [Panel] retains
 * its own mode and [ViewportState]. A panel instance is claimed by exactly one set for its
 * lifetime.
 */
public class PanelSet<K : PanelId, I> private constructor(
    /** The always-included derived-width panel, or null for a uniform set. */
    private val main: Panel<K, I>?,
    /** Panel declarations in layout order; already copied by the factory that built this set. */
    private val sides: List<Panel<K, I>>,
    private val reservedColumns: Int?,
    fixedWidthPanels: Set<K>,
) {
    private val fixedWidthPanels: Set<K> = HashSet(fixedWidthPanels)
    private var focusedId: K = main?.id ?: sides.first().id

    /**
     * The geometry and settled scroll of the last completed [Panels] frame, in frame coordinates,
     * or null before the first one. A failed frame leaves the previous layout in place.
     */
    public var layout: PanelLayout<K>? = null
        private set

    /**
     * The panel that receives focused input. Never absent: a set always owns at least one panel,
     * and a frame with nothing visible retains the focus it had rather than dropping it, so focus
     * returns where the user left it once panels become visible again.
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
        (if (id == main?.id) main else sides.firstOrNull { it.id == id })
            ?: throw IllegalArgumentException("Panel $id is not declared in this PanelSet")

    /** Focuses [id], demoting any other maximized panel. */
    public fun focus(id: K) {
        val panel = panelFor(id)
        allPanels().forEach { if (it !== panel) it.demoteFromMaximized() }
        focusedId = id
    }

    /** Focuses [id], or cycles it forward when it is already focused. */
    public fun focusOrCycle(id: K) {
        if (id == focusedId) {
            cycleFocusedMode(1)
        } else {
            focus(id)
        }
    }

    /** Cycles the focused panel's declared mode. */
    public fun cycleFocusedMode(delta: Int) {
        val panel = panelFor(focusedId)
        val oldMode = panel.mode
        panel.cycleMode(delta)
        if (panel.mode == PanelMode.MAXIMIZED && oldMode != PanelMode.MAXIMIZED) {
            allPanels().forEach { if (it !== panel) it.demoteFromMaximized() }
        }
    }

    public fun scrollFocused(dx: Int, dy: Int) {
        panelFor(focusedId).scrollBy(dx, dy)
    }

    /** Scrolls the focused panel by one viewport height; [direction] is -1 or +1. */
    public fun pageFocused(direction: Int) {
        val page = slotFor(focusedId)?.content?.height?.coerceAtLeast(1) ?: 1
        panelFor(focusedId).scrollBy(0, page * direction)
    }

    /** Mouse path: scrolls a specific panel regardless of focus. */
    public fun scroll(id: K, dx: Int, dy: Int) {
        panelFor(id).scrollBy(dx, dy)
    }

    /** Queues recentering for this panel's next drawable frame, including while hidden. */
    public fun requestRecenter(id: K) {
        panelFor(id).requestRecenter()
    }

    /** The panel at frame coordinate ([x], [y]) in the last completed frame, including the main panel. */
    public fun panelAt(x: Int, y: Int): K? = layout?.panelAt(x, y)?.id

    /**
     * Resolves frame coordinate ([x], [y]) against the last completed frame. Border and padding hits identify the
     * panel but have no content point; a content point is translated through the settled offset
     * and the scrolling panel's reclaimable top spacer.
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
     * mutate a panel through it.
     */
    public fun modeOf(id: K): PanelMode = panelFor(id).mode

    private fun slotFor(id: K): PanelLayout.Slot<K>? {
        val completed = layout ?: return null
        if (completed.main?.id == id) return completed.main
        return completed.sides.firstOrNull { it.id == id }
    }

    /**
     * The body of [Panels.render]: lays out [visible] panels over [canvas], selecting each rendered
     * presentation exactly once, then draws every slot and publishes [layout]. Width comes from
     * that same selected presentation; it is never recomputed by asking an application builder
     * again.
     */
    internal fun render(canvas: Canvas, inputs: I, visible: Set<K>) {
        val visibleSides = sides.filter { it.id in visible }
        normalizeFocus(visibleSides)

        val maximizedPanel = visibleSides.firstOrNull { it.mode == PanelMode.MAXIMIZED }
        val renderedPanels = if (maximizedPanel != null) {
            listOf(maximizedPanel)
        } else {
            buildList {
                main?.let(::add)
                addAll(visibleSides)
            }
        }
        val presentations = renderedPanels.associateBy({ it.id }, { it.presentation(inputs) })
        val widthOf: (Panel<K, I>) -> Int = { panel -> presentations.getValue(panel.id).requiredWidth() }
        val fixedPanels = fixedWidthPanels + visibleSides
            .filter { it.mode == PanelMode.MINIMIZED }
            .map { it.id }

        val local = if (main != null) {
            PanelLayout.compute(canvas.width, canvas.height, main, visibleSides, widthOf)
        } else {
            PanelLayout.computeUniform(
                canvas.width,
                canvas.height,
                visibleSides,
                (reservedColumns ?: visibleSides.count { it.id !in fixedWidthPanels }) -
                    visibleSides.count { it.id !in fixedWidthPanels && it.mode == PanelMode.MINIMIZED },
                fixedPanels,
                widthOf,
            )
        }
        val candidates = buildMap {
            for (slot in listOfNotNull(local.main) + local.sides) {
                put(slot.id, renderSlot(canvas, slot, presentations))
            }
        }
        val settled = local.withSettledScroll { id -> candidates.getValue(id).settled }
        candidates.forEach { (id, candidate) -> panelFor(id).settle(candidate) }
        layout = settled.offset(canvas.origin.x, canvas.origin.y)
    }

    private fun renderSlot(
        canvas: Canvas,
        slot: PanelLayout.Slot<K>,
        presentations: Map<K, Panel.Presentation>,
    ): ViewportState {
        return panelFor(slot.id).render(
            canvas.region(slot.outer),
            presentations.getValue(slot.id),
            focused = slot.id == focusedId,
        )
    }

    /**
     * Moves focus onto a visible panel when the focused one is hidden. A uniform frame with
     * nothing visible keeps its current focus: there is no better panel to move to, and dropping
     * it would silently relocate the user's focus to the first panel when visibility returns.
     */
    private fun normalizeFocus(visibleSides: List<Panel<K, I>>) {
        if (main != null) {
            val visible = focusedId == main.id || visibleSides.any { it.id == focusedId }
            if (!visible) focus(main.id)
            return
        }

        if (visibleSides.none { it.id == focusedId }) {
            visibleSides.firstOrNull()?.let { focus(it.id) }
        }
    }

    private fun allPanels(): List<Panel<K, I>> = buildList {
        main?.let(::add)
        addAll(sides)
    }

    public companion object {
        /**
         * Builds a uniform set. By default, visible proportional panels share the remaining width.
         * [reservedColumns] reserves proportional slots even for hidden panels; visible minimized
         * proportional panels reclaim their slot. [fixedWidthPanels] occupy a trailing fixed group.
         * Configuration is copied and validated before any panel is attached.
         */
        public fun <K : PanelId, I> uniform(
            panels: List<Panel<K, I>>,
            reservedColumns: Int? = null,
            fixedWidthPanels: Set<K> = emptySet(),
        ): PanelSet<K, I> {
            require(panels.isNotEmpty()) { "A uniform PanelSet needs at least one panel" }
            require(reservedColumns == null || reservedColumns >= 0) { "Reserved columns must not be negative" }
            require(fixedWidthPanels.all { id -> panels.any { it.id == id } }) {
                "Fixed-width panels must be declared in the set"
            }
            require(reservedColumns == null || reservedColumns >= panels.count { it.id !in fixedWidthPanels }) {
                "Reserve at least one column per proportional panel"
            }
            return create(null, panels, reservedColumns, fixedWidthPanels)
        }

        /** Builds a derived-main set. The main panel may only declare NORMAL. */
        public fun <K : PanelId, I> mainAndSides(
            main: Panel<K, I>,
            sides: List<Panel<K, I>>,
        ): PanelSet<K, I> = create(main, sides)

        private fun <K : PanelId, I> create(
            main: Panel<K, I>?,
            sides: List<Panel<K, I>>,
            reservedColumns: Int? = null,
            fixedWidthPanels: Set<K> = emptySet(),
        ): PanelSet<K, I> {
            require(main != null || sides.isNotEmpty()) { "A PanelSet needs at least one panel" }
            if (main != null) {
                require(main.modes == listOf(PanelMode.NORMAL)) {
                    "Main panel ${main.id} must declare NORMAL only"
                }
            }

            val copiedSides = sides.toList()
            val all = buildList {
                main?.let(::add)
                addAll(copiedSides)
            }
            val ids = HashSet<K>()
            all.forEach { panel ->
                require(ids.add(panel.id)) { "Panel id ${panel.id} appears more than once in this PanelSet" }
                panel.requireUnattached()
            }

            all.forEach { it.claimAttachment() }
            return PanelSet(main, copiedSides, reservedColumns, fixedWidthPanels)
        }
    }
}
