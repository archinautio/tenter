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
 * A box border with an optional title/badge in the top border, in the manner of Ratatui's
 * `Block`. A block paints only its own chrome: render the content separately into [inner].
 * Scrollbar thumbs are a scrolling-panel concern and are painted by [ScrollingPanel] from its
 * settled [ScrollState], so a reusable border has no scrolling knowledge.
 * [padding] is extra space consumed between the border and the content, beyond the 1-cell border
 * itself (e.g. the horizontal breathing room a scrolling panel's viewport wants); pass
 * [Padding.ZERO] for a border with nothing but the border between it and content.
 *
 * The single place every framed widget gets its box from — no widget should draw its own border.
 */
public class Block(
    private val title: String = "",
    private val badge: String? = null,
    private val padding: Padding = Padding.ZERO,
    private val borderColor: ColorRole = ChromeRole.PANEL_BORDER,
    private val titleColor: ColorRole = ChromeRole.ACCENT,
) : Widget {

    /** The content area of a block rendered into [area]: [area] minus the border and [padding]. */
    public fun inner(area: Rect): Rect = area.inner(BORDER + padding)

    /** The content canvas of a block rendered into [canvas]: minus the border and [padding]. */
    public fun inner(canvas: Canvas): Canvas = canvas.inner(BORDER + padding)

    public override fun render(canvas: Canvas) {
        drawBorder(canvas)
    }

    /** No-op below 2x2. */
    private fun drawBorder(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        if (width < 2 || height < 2) return

        canvas.set(0, 0, Cell("╭", Style(borderColor)))
        canvas.set(width - 1, 0, Cell("╮", Style(borderColor)))
        canvas.set(0, height - 1, Cell("╰", Style(borderColor)))
        canvas.set(width - 1, height - 1, Cell("╯", Style(borderColor)))

        for (i in 1 until width - 1) {
            canvas.set(i, 0, Cell("─", Style(borderColor)))
            canvas.set(i, height - 1, Cell("─", Style(borderColor)))
        }

        for (i in 1 until height - 1) {
            canvas.set(0, i, Cell("│", Style(borderColor)))
            canvas.set(width - 1, i, Cell("│", Style(borderColor)))
        }

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
     * border (`.x > 0`), at the ranges [ScrollGeometry.thumb] computes from [scroll]'s settled
     * values and this box's own viewport size — the same region [inner] gives the content.
     */
    internal fun drawThumbs(canvas: Canvas, scroll: ScrollState) {
        val inset = BORDER + padding
        val viewportWidth = canvas.width - inset.left - inset.right
        val viewportHeight = canvas.height - inset.top - inset.bottom

        val thumbStyle = Style(borderColor)

        ScrollGeometry.thumb(
            track = viewportHeight,
            contentLength = scroll.maxOffset.y + viewportHeight,
            viewportLength = viewportHeight,
            offset = scroll.offset.y,
        )?.forEach { i -> canvas.set(canvas.width - 1, inset.top + i, Cell("▐", thumbStyle)) }

        ScrollGeometry.thumb(
            track = viewportWidth,
            contentLength = scroll.maxOffset.x + viewportWidth,
            viewportLength = viewportWidth,
            offset = scroll.offset.x,
        )?.forEach { i -> canvas.set(inset.left + i, canvas.height - 1, Cell("▬", thumbStyle)) }
    }

    public companion object {
        /** Composes an intrinsic border around prepared content; [Block] itself paints an allocated canvas. */
        public fun prepared(
            content: ContentWidget,
            title: String = "",
            badge: String? = null,
            padding: Padding = Padding.ZERO,
            borderColor: ColorRole = ChromeRole.PANEL_BORDER,
            titleColor: ColorRole = ChromeRole.ACCENT,
        ): ContentWidget = object : ContentWidget {
            public override fun layout(availableWidth: Int): ContentLayout {
                require(availableWidth >= 0) { "available width must not be negative: $availableWidth" }
                val outer = BORDER + padding
                val innerWidth = (availableWidth.toLong() - outer.left - outer.right).coerceAtLeast(0).toInt()
                val child = content.layout(innerWidth)
                val width = checkedAdd(checkedAdd(child.width, outer.left, BLOCK_SIZE), outer.right, BLOCK_SIZE)
                val height = checkedAdd(checkedAdd(child.height, outer.top, BLOCK_SIZE), outer.bottom, BLOCK_SIZE)
                val block = Block(title, badge, padding, borderColor, titleColor)
                return ContentLayout.raw(width, height, object : Widget {
                    override fun render(canvas: Canvas) {
                        block.render(canvas)
                        child.render(block.inner(canvas))
                    }
                })
            }
        }

        private const val BLOCK_SIZE: String = "block size"

        /** One cell on each side, consumed by every [Block] box. */
        public val BORDER: Padding = Padding.uniform(1)

        /**
         * Breathing room between the border and the content. `top = 1` is the blank spacer row
         * under the title, uniform with the left/right padding; `bottom = 0` leaves content
         * flush against the bottom border.
         *
         * The horizontal padding is frame — fixed for a scrolling panel's lifetime — but [top]
         * is NOT: it is prepended to the content stream (see [scrollingPanel]) rather than baked
         * into the viewport, so it is visible at rest and the content reclaims its row the moment
         * the user scrolls.
         */
        internal val PADDING: Padding = Padding(left = 1, top = 1, right = 1)

        /**
         * A scrolling panel's viewport: border plus the horizontal padding, at full inner height.
         * Deliberately not public: a caller translating a screen click into content-stream
         * coordinates asks [io.archinaut.tenter.panel.PanelSet.hitTest], which reads the completed frame's own
         * geometry, rather than re-deriving it from this constant and a scroll offset.
         */
        internal val VIEWPORT_INSET: Padding = BORDER + PADDING.horizontalEdges()
    }
}
