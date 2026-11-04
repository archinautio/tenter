// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.screen.offset
import io.archinaut.tenter.text.CellWidth
import io.archinaut.tenter.text.TextTruncation

/**
 * Borders, a [title] on the top row, and padding; it paints only this chrome, around the content
 * rendered into [inner]. The title sits on the top border, or on its own row without one.
 * [borderType]'s head section gives the top row, its body the sides, and its foot the bottom row.
 *
 * The title is placed by its alignment, clear of the corners; [TextAlign.NONE] and
 * [TextAlign.JUSTIFY] place it as [TextAlign.LEFT] does. Text too wide for the row ends in an
 * ellipsis, and when not even its first glyph fits, a badge shows alone, as on a minimized stub.
 */
public class Block(
    private val title: Title? = null,
    private val borders: Borders = Borders.NONE,
    private val padding: Padding = Padding.ZERO,
    private val borderColor: ColorRole = StandardRole.PANEL_BORDER,
    private val titleColor: ColorRole = StandardRole.ACCENT,
    private val borderType: BorderType = BorderType.ROUNDED,
) : Widget {

    /** Everything this block consumes on each side: borders (or the title row) plus [padding]. */
    internal val insets: Padding = Padding(
        left = if (borders.left) 1 else 0,
        top = if (borders.top || title?.isEmpty == false) 1 else 0,
        right = if (borders.right) 1 else 0,
        bottom = if (borders.bottom) 1 else 0,
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

        drawTitle(canvas)
    }

    private fun drawTitle(canvas: Canvas) {
        val title = title ?: return
        val (margin, run) = titleRun(title, canvas.width) ?: return
        var column = margin + title.align.offset(run.width, canvas.width - 2 * margin)
        for (span in run.spans) column = canvas.setString(column, 0, span.text, span.style).x
    }

    /**
     * What of [title] fits on the top row of a [width]-cell block, and the cells kept clear of each
     * corner: `" text "`, `"[badge] text "`, or the badge alone.
     */
    private fun titleRun(title: Title, width: Int): Pair<Int, Line>? {
        val titleStyle = Style(titleColor)
        val gap = Span(" ", Style(borderColor))
        val text = Line(title.line.spans.map { Span(it.text, titleStyle.patch(it.style)) })
        val badge = title.badge?.let { Span("[$it]", titleStyle) }
        if (text.width > 0) {
            if (badge == null) {
                fitted(text, width - 7)?.let { return 3 to Line(gap) + it + Line(gap) }
            } else {
                fitted(text, width - CellWidth.of(badge.text) - 6)?.let {
                    return 2 to Line(badge, Span(" ", titleStyle)) + it + Line(gap)
                }
            }
        }
        return badge?.takeIf { width > 2 + CellWidth.of(it.text) }?.let { 2 to Line(it) }
    }

    /** [text] within [maxWidth], ellipsized if need be, or null when not even its first glyph fits. */
    private fun fitted(text: Line, maxWidth: Int): Line? =
        if (text.width <= maxWidth) text else text.ellipsize(maxWidth).takeIf { it.width > ELLIPSIS_WIDTH }

    public companion object {
        /** A block with all four borders. */
        public fun bordered(
            title: Title? = null,
            padding: Padding = Padding.ZERO,
            borderColor: ColorRole = StandardRole.PANEL_BORDER,
            titleColor: ColorRole = StandardRole.ACCENT,
            borderType: BorderType = BorderType.ROUNDED,
        ): Block = Block(title, Borders.ALL, padding, borderColor, titleColor, borderType)

        private val ELLIPSIS_WIDTH: Int = CellWidth.of(TextTruncation.ELLIPSIS)
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
