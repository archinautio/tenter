// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import java.util.Collections
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.ScrollState

/**
 * The rendered area and every placed panel's immutable geometry from one completed frame of
 * [Panels]. Coordinates are absolute frame coordinates, the same space as terminal mouse events,
 * whatever area the panels were rendered into.
 */
public class PanelLayout<K : PanelId> private constructor(
    /** The whole area the panels were rendered into — where a maximized panel goes. */
    public val area: Rect,
    /** The main panel's slot, or null for a uniform layout. */
    public val main: Slot<K>?,
    /** Uniform-layout slots in declaration order. */
    sides: List<Slot<K>>,
) {
    public val sides: List<Slot<K>> = Collections.unmodifiableList(ArrayList(sides))

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

    /** The panel whose painted outer rectangle contains ([x], [y]), or null. */
    internal fun panelAt(x: Int, y: Int): Slot<K>? = buildList {
        main?.let(::add)
        addAll(sides)
    }.firstOrNull { it.outer.contains(x, y) }

    internal fun withSettledScroll(scrollOf: (K) -> ScrollState?): PanelLayout<K> = PanelLayout(
        area = area,
        main = main?.withScroll(scrollOf(main.id)),
        sides = sides.map { it.withScroll(scrollOf(it.id)) },
    )

    /** This layout moved by ([dx], [dy]) — from canvas-local to frame coordinates. */
    internal fun offset(dx: Int, dy: Int): PanelLayout<K> = PanelLayout(
        area = area.offset(dx, dy),
        main = main?.offset(dx, dy),
        sides = sides.map { it.offset(dx, dy) },
    )

    private fun Slot<K>.offset(dx: Int, dy: Int): Slot<K> = copy(outer = outer.offset(dx, dy), content = content.offset(dx, dy))

    private fun Slot<K>.withScroll(scroll: ScrollState?): Slot<K> = copy(
        scroll = if (content.width > 0 && content.height > 0) scroll else null,
    )

    public companion object {
        /**
         * Lays out [sides] and [main] over a [width]x[height] canvas. Side widths are reserved first;
         * if they do not fit, the main slot becomes zero-width and sides are clipped in declaration order.
         */
        internal fun <K : PanelId, I> compute(
            width: Int,
            height: Int,
            main: Panel<K, I>,
            sides: List<Panel<K, I>>,
            widthOf: (Panel<K, I>) -> Int,
        ): PanelLayout<K> {
            validateCanvas(width, height)
            val area = Rect(0, 0, width, height)
            sides.firstOrNull { it.mode == PanelMode.MAXIMIZED }?.let { maximized ->
                return PanelLayout(
                    area = area,
                    main = null,
                    sides = listOf(slot(maximized.id, 0, width, height, width)),
                )
            }
            val sideWidths = sides.map { panel -> checkedWidth(widthOf(panel), panel.id) }
            val totalSideWidth = sides.indices.sumOf { sideWidths[it].toLong() }
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            val mainWidth = (width - totalSideWidth).coerceAtLeast(0)
            val slots = buildList {
                var nextX = if (totalSideWidth <= width) mainWidth else 0
                for ((index, panel) in sides.withIndex()) {
                    val available = (width - nextX).coerceAtLeast(0)
                    val slotWidth = sideWidths[index].coerceAtMost(available)
                    add(slot(panel.id, nextX, slotWidth, height, width))
                    nextX += slotWidth
                }
            }
            return PanelLayout(
                area = area,
                main = slot(main.id, 0, mainWidth, height, width),
                sides = slots,
            )
        }

        /**
         * Lays [panels] out over a [width]x[height] canvas as proportional columns with an optional
         * trailing fixed group.
         * Minimized panels are fixed-width, but remain at their declaration positions among the
         * proportional panels. Other fixed panels form a trailing group in declaration order.
         */
        internal fun <K : PanelId, I> computeUniform(
            width: Int,
            height: Int,
            panels: List<Panel<K, I>>,
            columnCount: Int = panels.size,
            fixedWidthPanels: Set<K> = emptySet(),
            widthOf: (Panel<K, I>) -> Int,
        ): PanelLayout<K> {
            validateCanvas(width, height)
            require(columnCount >= 0) { "uniform column count must not be negative: $columnCount" }
            val area = Rect(0, 0, width, height)
            if (panels.isEmpty()) return PanelLayout(area, null, emptyList())

            panels.firstOrNull { it.mode == PanelMode.MAXIMIZED }?.let { maximized ->
                return PanelLayout(
                    area = area,
                    main = null,
                    sides = listOf(slot(maximized.id, 0, width, height, width)),
                )
            }

            val fixed = panels.filter { it.id in fixedWidthPanels || it.mode == PanelMode.MINIMIZED }
            val proportional = panels.filter { it !in fixed }
            require(columnCount >= proportional.size || proportional.isEmpty()) {
                "A uniform layout needs at least one column per proportional panel"
            }
            val widths = fixed.associate { it.id to checkedWidth(widthOf(it), it.id) }
            val fixedWidth = fixed.sumOf { widths.getValue(it.id).toLong() }
            val proportionalWidth = (width.toLong() - fixedWidth)
                .coerceAtLeast(0)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            val columnWidths = proportionalWidths(proportionalWidth, columnCount, proportional.size)
            val minimizedWidth = fixed
                .filter { it.mode == PanelMode.MINIMIZED }
                .sumOf { widths.getValue(it.id).toLong() }
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()

            val slots = buildList {
                var proportionalIndex = 0
                var x = 0
                for (panel in panels) {
                    if (panel in proportional) {
                        val panelWidth = columnWidths[proportionalIndex]
                        add(slot(panel.id, x, panelWidth, height, width))
                        x += panelWidth
                        proportionalIndex++
                    } else if (panel.mode == PanelMode.MINIMIZED) {
                        val panelWidth = widths.getValue(panel.id).coerceAtMost((width - x).coerceAtLeast(0))
                        add(slot(panel.id, x, panelWidth, height, width))
                        x += panelWidth
                    }
                }
                x = proportionalWidth + minimizedWidth
                for (panel in fixed) {
                    if (panel.mode != PanelMode.MINIMIZED) {
                        val panelWidth = widths.getValue(panel.id).coerceAtMost((width - x).coerceAtLeast(0))
                        add(slot(panel.id, x, panelWidth, height, width))
                        x += panelWidth
                    }
                }
            }
            return PanelLayout(area, null, slots)
        }

        private fun <K : PanelId> slot(
            id: K,
            x: Int,
            width: Int,
            height: Int,
            canvasWidth: Int,
        ): Slot<K> {
            val outer = Rect(x.coerceIn(0, canvasWidth), 0, width, height)
            return Slot(
                id = id,
                outer = outer,
                content = outer.inner(Block.VIEWPORT_INSET),
                scroll = null,
            )
        }

        private fun proportionalWidths(width: Int, columnCount: Int, panelCount: Int): List<Int> {
            if (panelCount == 0) return emptyList()
            val base = if (columnCount == 0) 0 else width / columnCount
            val remainder = if (columnCount == 0) 0 else width % columnCount
            return List(panelCount) { index -> base + if (index < remainder) 1 else 0 }
        }

        private fun validateCanvas(width: Int, height: Int) {
            require(width >= 0) { "canvas width must not be negative: $width" }
            require(height >= 0) { "canvas height must not be negative: $height" }
        }

        private fun checkedWidth(width: Int, id: PanelId): Int {
            require(width >= 0) { "Panel $id width must not be negative: $width" }
            return width
        }
    }
}
