// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

/**
 * Borders, [titles] on the top and bottom rows, and padding; it paints only this chrome, around
 * the content rendered into [inner]. A title sits on its border, or on its own row without one.
 * [borderType]'s head section gives the top row, its body the sides, and its foot the bottom row.
 *
 * Titles keep the corner and one border cell clear of each edge. [TextAlign.NONE] and
 * [TextAlign.JUSTIFY] place a title as [TextAlign.LEFT] does. When a row is too narrow, earlier
 * titles keep their room: a later one is ellipsized, then dropped once not even its first glyph fits.
 */
public class Block(
    private val titles: List<Title> = emptyList(),
    private val borders: Borders = Borders.NONE,
    private val padding: Padding = Padding.ZERO,
    private val borderColor: ColorRole = StandardRole.PANEL_BORDER,
    private val titleColor: ColorRole = StandardRole.ACCENT,
    private val borderType: BorderType = BorderType.ROUNDED,
) : Widget {

    /** Everything this block consumes on each side: borders (or title rows) plus [padding]. */
    internal val insets: Padding = Padding(
        left = if (borders.left) 1 else 0,
        top = if (borders.top || hasTitle(TitlePosition.TOP)) 1 else 0,
        right = if (borders.right) 1 else 0,
        bottom = if (borders.bottom || hasTitle(TitlePosition.BOTTOM)) 1 else 0,
    ) + padding

    /** [area] without the borders and [padding]. */
    public fun inner(area: Rect): Rect = area.inner(insets)

    /** [canvas] without the borders and [padding]. */
    public fun inner(canvas: Canvas): Canvas = canvas.inner(insets)

    public override fun render(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        if (width == 0 || height == 0) return
        if ((borders.left && borders.right && width < 2) || (borders.top && borders.bottom && height < 2)) return

        val style = Style(borderColor)
        val head = borderType.head
        val body = borderType.body
        val foot = borderType.foot
        for (i in 0 until width) {
            if (borders.top) canvas.set(i, 0, head.ew, style)
            if (borders.bottom) canvas.set(i, height - 1, foot.ew, style)
        }
        for (i in 0 until height) {
            if (borders.left) canvas.set(0, i, body.ns, style)
            if (borders.right) canvas.set(width - 1, i, body.ns, style)
        }
        if (borders.top && borders.left) canvas.set(0, 0, head.es, style)
        if (borders.top && borders.right) canvas.set(width - 1, 0, head.sw, style)
        if (borders.bottom && borders.left) canvas.set(0, height - 1, foot.ne, style)
        if (borders.bottom && borders.right) canvas.set(width - 1, height - 1, foot.nw, style)

        drawTitles(canvas, TitlePosition.TOP, 0)
        drawTitles(canvas, TitlePosition.BOTTOM, height - 1)
    }

    private fun hasTitle(position: TitlePosition): Boolean = titles.any { it.position == position && !it.isEmpty }

    private fun drawTitles(canvas: Canvas, position: TitlePosition, y: Int) {
        val row = titles.filter { it.position == position }
        for (placed in placeTitles(row, canvas.width, Style(titleColor), Style(borderColor))) {
            var column = placed.column
            for (span in placed.run.spans) column = canvas.setString(column, y, span.text, span.style).x
        }
    }

    public companion object {
        /** A block with all four borders. */
        public fun bordered(
            titles: List<Title> = emptyList(),
            padding: Padding = Padding.ZERO,
            borderColor: ColorRole = StandardRole.PANEL_BORDER,
            titleColor: ColorRole = StandardRole.ACCENT,
            borderType: BorderType = BorderType.ROUNDED,
        ): Block = Block(titles, Borders.ALL, padding, borderColor, titleColor, borderType)
    }
}

/** This content surrounded by [block]'s borders, title, and padding, still measurable. */
public fun MeasurableWidget.block(block: Block): MeasurableWidget {
    val content = this
    return object : LayoutWidget {
        override fun layout(availableWidth: Int): ContentLayout {
            val insets = block.insets
            val innerWidth = (availableWidth.toLong() - insets.left - insets.right).coerceAtLeast(0).toInt()
            val child = content.prepare(innerWidth)
            val width = checkedAdd(checkedAdd(child.width, insets.left, BLOCK_SIZE), insets.right, BLOCK_SIZE)
            val height = checkedAdd(checkedAdd(child.height, insets.top, BLOCK_SIZE), insets.bottom, BLOCK_SIZE)
            return contentLayout(width, height) {
                paint(block)
                place(insets.left, insets.top, child)
            }
        }
    }
}

private const val BLOCK_SIZE: String = "block size"
