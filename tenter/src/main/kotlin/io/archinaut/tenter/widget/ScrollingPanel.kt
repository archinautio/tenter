// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.screen.Canvas

/**
 * A bordered, scrolling panel: a bordered [Block] for the box, [Viewport] for the scroll math, and
 * a borderless [Block] for the reclaimable top spacer row, wired together exactly once. Exists as
 * its own type (rather than callers composing a [Block] and a [Viewport]) because painting the
 * scrollbar thumbs has to happen *after* the viewport has settled, and a border knows nothing
 * about scrolling. Read the settled result from the [ViewportState] you render it with.
 *
 * The vertical component of [Block.PADDING] is folded into the content stream rather than the
 * viewport, which is what makes it a spacer at rest that the content reclaims the moment the
 * content scrolls. The horizontal component becomes the bordered [Block]'s padding, a pure
 * viewport concern.
 */
public class ScrollingPanel(
    content: MeasurableWidget,
    title: String = "",
    badge: String? = null,
    borderColor: ColorRole = ChromeRole.PANEL_BORDER,
    titleColor: ColorRole = ChromeRole.ACCENT,
) : StatefulWidget<ViewportState> {

    private val block = Block.bordered(
        title = title,
        badge = badge,
        padding = Block.PADDING.horizontalEdges(),
        borderColor = borderColor,
        titleColor = titleColor,
    )

    private val viewport = Viewport(content.block(Block(padding = Block.PADDING.verticalEdges())))

    override fun render(canvas: Canvas, state: ViewportState) {
        block.render(canvas)
        viewport.render(block.inner(canvas), state)
        state.settled?.let { block.drawThumbs(canvas, it) }
    }
}
