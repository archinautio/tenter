// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.palette.ColorRole

/**
 * A bordered, scrolling panel — [Block] for the box and [Viewport] for the scroll math, wired
 * together by [scrollingPanel]. Exists as its own type (rather than callers just getting a
 * [Block] back) because painting the scrollbar thumbs has to happen *after* the viewport has
 * settled, and a border knows nothing about scrolling. Read the settled result from the
 * [ViewportState] you render it with.
 */
public class ScrollingPanel internal constructor(
    private val block: Block,
    private val viewport: Viewport,
) : StatefulWidget<ViewportState> {

    override fun render(canvas: Canvas, state: ViewportState) {
        block.render(canvas)
        viewport.render(block.inner(canvas), state)
        state.settled?.let { block.drawThumbs(canvas, it) }
    }
}

/**
 * Composes a [ScrollingPanel]: [Block] for the box, [Viewport] for the scroll math, and
 * [Padded] for the reclaimable top spacer row — the single construction site every scrolling
 * panel goes through, so the three decorators are wired together exactly once.
 *
 * The vertical component of [Block.PADDING] is folded into the content stream via [Padded]
 * rather than the viewport, which is
 * what makes it a spacer at rest that the content reclaims the moment the content scrolls. The
 * horizontal component becomes the [Block]'s padding, a pure viewport concern.
 */
public fun scrollingPanel(
    title: String,
    badge: String?,
    content: ContentWidget,
    borderColor: ColorRole = ChromeRole.PANEL_BORDER,
    titleColor: ColorRole = ChromeRole.ACCENT,
): ScrollingPanel {
    val paddedContent = Padded.prepared(Block.PADDING.verticalEdges(), content)
    val block = Block(
        title = title,
        badge = badge,
        padding = Block.PADDING.horizontalEdges(),
        borderColor = borderColor,
        titleColor = titleColor,
    )
    return ScrollingPanel(block, Viewport(paddedContent))
}
