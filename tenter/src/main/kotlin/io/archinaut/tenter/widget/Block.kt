// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.text.CellWidth

/**
 * Borders, an optional title/badge on the top row, and padding. `Block()` has no border;
 * [bordered] draws all four sides. A block without borders is how content gets padding alone.
 *
 * A block paints only its own chrome. In an allocated area, render it and then render the content
 * into [inner]; around measurable content, use [MeasurableWidget.block]. The title sits on the top
 * border, or on its own top row when there is no top border. [padding] is space between the
 * borders and the content.
 *
 * The single place every framed widget gets its box from — no widget should draw its own border.
 * A [Viewport] given a block paints scrollbar thumbs on it from its settled [ScrollState].
 */
public class Block(
    private val title: String = "",
    private val badge: String? = null,
    private val borders: Borders = Borders.NONE,
    private val padding: Padding = Padding.ZERO,
    private val borderColor: ColorRole = ChromeRole.PANEL_BORDER,
    private val titleColor: ColorRole = ChromeRole.ACCENT,
) : Widget {

    /** Everything this block consumes on each side: borders (or the title row) plus [padding]. */
    internal val insets: Padding = Padding(
        left = if (borders.left) 1 else 0,
        top = if (borders.top || title.isNotEmpty() || badge != null) 1 else 0,
        right = if (borders.right) 1 else 0,
        bottom = if (borders.bottom) 1 else 0,
    ) + padding

    /** The content area of a block rendered into [area]: [area] minus the borders and [padding]. */
    public fun inner(area: Rect): Rect = area.inner(insets)

    /** The content canvas of a block rendered into [canvas]: minus the borders and [padding]. */
    public fun inner(canvas: Canvas): Canvas = canvas.inner(insets)

    public override fun render(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        if (width == 0 || height == 0) return
        if ((borders.left && borders.right && width < 2) || (borders.top && borders.bottom && height < 2)) return

        val style = Style(borderColor)
        for (i in 0 until width) {
            if (borders.top) canvas.set(i, 0, Cell("─", style))
            if (borders.bottom) canvas.set(i, height - 1, Cell("─", style))
        }
        for (i in 0 until height) {
            if (borders.left) canvas.set(0, i, Cell("│", style))
            if (borders.right) canvas.set(width - 1, i, Cell("│", style))
        }
        if (borders.top && borders.left) canvas.set(0, 0, Cell("╭", style))
        if (borders.top && borders.right) canvas.set(width - 1, 0, Cell("╮", style))
        if (borders.bottom && borders.left) canvas.set(0, height - 1, Cell("╰", style))
        if (borders.bottom && borders.right) canvas.set(width - 1, height - 1, Cell("╯", style))

        drawTitle(canvas)
    }

    private fun drawTitle(canvas: Canvas) {
        val width = canvas.width
        val titleWidth = CellWidth.of(title)
        val badgeWidth = badge?.let(CellWidth::of) ?: 0
        val badgeRunWidth = badge?.let { CellWidth.of("[$it]") } ?: 0
        if (title.isNotEmpty()) {
            if (badge != null && width > titleWidth + badgeWidth + 7) {
                canvas.setString(2, 0, "[$badge] $title", Style(titleColor))
                canvas.set(5 + badgeWidth + titleWidth, 0, Cell(style = Style(borderColor)))
            } else if (badge == null && width > titleWidth + 6) {
                canvas.set(3, 0, Cell(style = Style(borderColor)))
                canvas.setString(4, 0, title, Style(titleColor))
                canvas.set(4 + titleWidth, 0, Cell(style = Style(borderColor)))
            } else if (badge != null && width > 2 + badgeRunWidth) {
                // The full "[badge] title" run doesn't fit — e.g. a minimized stub too narrow for
                // its full title. The badge alone is still worth showing.
                canvas.setString(2, 0, "[$badge]", Style(titleColor))
            }
        } else if (badge != null && width > 2 + badgeRunWidth) {
            // No title to anchor a "[badge] title" run to. The badge alone is still worth showing.
            canvas.setString(2, 0, "[$badge]", Style(titleColor))
        }
    }

    /**
     * Draws a scrollbar thumb on the right border ([ScrollState.maxOffset]`.y > 0`) and/or bottom
     * border (`.x > 0`), on whichever of those sides this block has a border, at the ranges [ScrollGeometry.thumb] computes from [scroll]'s settled
     * values and this box's own viewport size — the same region [inner] gives the content.
     */
    internal fun drawThumbs(canvas: Canvas, scroll: ScrollState) {
        val viewportWidth = canvas.width - insets.left - insets.right
        val viewportHeight = canvas.height - insets.top - insets.bottom

        val thumbStyle = Style(borderColor)

        if (borders.right) {
            ScrollGeometry.thumb(
                track = viewportHeight,
                contentLength = scroll.maxOffset.y + viewportHeight,
                viewportLength = viewportHeight,
                offset = scroll.offset.y,
            )?.forEach { i -> canvas.set(canvas.width - 1, insets.top + i, Cell("▐", thumbStyle)) }
        }

        if (borders.bottom) {
            ScrollGeometry.thumb(
                track = viewportWidth,
                contentLength = scroll.maxOffset.x + viewportWidth,
                viewportLength = viewportWidth,
                offset = scroll.offset.x,
            )?.forEach { i -> canvas.set(insets.left + i, canvas.height - 1, Cell("▬", thumbStyle)) }
        }
    }

    public companion object {
        /** A block with all four borders. */
        public fun bordered(
            title: String = "",
            badge: String? = null,
            padding: Padding = Padding.ZERO,
            borderColor: ColorRole = ChromeRole.PANEL_BORDER,
            titleColor: ColorRole = ChromeRole.ACCENT,
        ): Block = Block(title, badge, Borders.ALL, padding, borderColor, titleColor)

        /** One cell on each side, consumed by every bordered [Block] box. */
        internal val BORDER: Padding = Padding.uniform(1)

        /**
         * Breathing room between the border and the content. `top = 1` is the blank spacer row
         * under the title, uniform with the left/right padding; `bottom = 0` leaves content
         * flush against the bottom border.
         *
         * The horizontal padding is frame — fixed for a scrolling panel's lifetime — but [top]
         * is NOT: it is prepended to the content stream (see [io.archinaut.tenter.widget.panel.Panel]) rather than baked
         * into the viewport, so it is visible at rest and the content reclaims its row the moment
         * the user scrolls.
         */
        internal val PADDING: Padding = Padding(left = 1, top = 1, right = 1)

        /**
         * A scrolling panel's viewport: border plus the horizontal padding, at full inner height.
         * Deliberately not public: a caller translating a screen click into content-stream
         * coordinates asks [io.archinaut.tenter.widget.panel.PanelSet.hitTest], which reads the completed frame's own
         * geometry, rather than re-deriving it from this constant and a scroll offset.
         */
        internal val VIEWPORT_INSET: Padding = BORDER + PADDING.horizontalEdges()
    }
}

/**
 * This content surrounded by [block]'s borders, title, and padding. The result is measurable: its
 * width is the content's plus the block's sides, its height the content's plus the block's top and
 * bottom.
 */
public fun MeasurableWidget.block(block: Block): MeasurableWidget {
    val content = this
    return object : LayoutWidget {
        override fun layout(availableWidth: Int): ContentLayout {
            require(availableWidth >= 0) { "available width must not be negative: $availableWidth" }
            val insets = block.insets
            val innerWidth = (availableWidth.toLong() - insets.left - insets.right).coerceAtLeast(0).toInt()
            val child = content.prepare(innerWidth)
            val width = checkedAdd(checkedAdd(child.width, insets.left, BLOCK_SIZE), insets.right, BLOCK_SIZE)
            val height = checkedAdd(checkedAdd(child.height, insets.top, BLOCK_SIZE), insets.bottom, BLOCK_SIZE)
            return contentLayout(width, height) {
                decorate(block)
                place(insets.left, insets.top, child)
            }
        }
    }
}

private const val BLOCK_SIZE: String = "block size"
