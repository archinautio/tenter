// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import com.github.ajalt.mordant.rendering.BorderType
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Scrollbar
import io.archinaut.tenter.widget.Viewport
import io.archinaut.tenter.widget.ViewportState
import io.archinaut.tenter.widget.block

/**
 * One panel declaration: a bordered, scrolling frame around the content its mode's builder returns
 * each frame. It keeps its mode and scroll position, which only its [PanelSet] changes. It has no
 * size; the application gives it an area every frame through [Panels].
 */
public class Panel<K : PanelId, I>(
    internal val id: K,
    private val title: String,
    private val badge: String? = null,
    private val normal: (I) -> MeasurableWidget,
    private val minimized: ((I) -> MeasurableWidget)? = null,
    private val maximized: ((I) -> MeasurableWidget)? = null,
    private val borderType: BorderType = BorderType.ROUNDED,
) {
    private val viewportState: ViewportState = ViewportState()
    private var restoreMode: PanelMode = PanelMode.NORMAL
    private var attached: Boolean = false

    internal var mode: PanelMode = PanelMode.NORMAL
        private set

    /** The declared modes, smallest first. */
    internal val modes: List<PanelMode> = listOfNotNull(
        minimized?.let { PanelMode.MINIMIZED },
        PanelMode.NORMAL,
        maximized?.let { PanelMode.MAXIMIZED },
    )

    /** Steps [delta] through the declared modes, wrapping, and remembers the mode a maximize left. */
    internal fun cycleMode(delta: Int) {
        val index = modes.indexOf(mode)
        val next = modes[(index + delta).mod(modes.size)]
        if (next == PanelMode.MAXIMIZED && mode != PanelMode.MAXIMIZED) restoreMode = mode
        mode = next
    }

    /** Returns to the mode this panel was maximized from. */
    internal fun demoteFromMaximized() {
        if (mode == PanelMode.MAXIMIZED) mode = restoreMode
    }

    internal fun scrollBy(dx: Int, dy: Int) {
        viewportState.scrollBy(dx, dy)
    }

    internal fun requestRecenter() {
        viewportState.requestRecenter()
    }

    internal fun content(inputs: I): MeasurableWidget = when (mode) {
        PanelMode.MINIMIZED -> minimized ?: error("Panel $id is in MINIMIZED mode but declares no minimized content")
        PanelMode.NORMAL -> normal
        PanelMode.MAXIMIZED -> maximized ?: error("Panel $id is in MAXIMIZED mode but declares no maximized content")
    }.invoke(inputs)

    internal fun claimAttachment() {
        require(!attached) { "Panel $id is already attached to a PanelSet" }
        attached = true
    }

    internal fun requireUnattached() {
        require(!attached) { "Panel $id is already attached to a PanelSet" }
    }

    /** Renders [content] into [canvas] and returns what it painted, in [canvas]'s coordinates. */
    internal fun render(
        canvas: Canvas,
        content: MeasurableWidget,
        focused: Boolean,
    ): PanelLayout.Slot<K> {
        val role = if (focused) StandardRole.PANEL_BORDER_FOCUSED else StandardRole.PANEL_BORDER
        val frame = Block.bordered(
            title = title,
            badge = badge,
            padding = Padding.horizontal(FRAME_PADDING),
            borderColor = role,
            titleColor = role,
            borderType = borderType,
        )
        frame.render(canvas)
        val viewportArea = frame.inner(canvas.area)
        val outer = PanelLayout.Slot(id, outer = canvas.area, content = viewportArea, scroll = null, contentBounds = null)
        if (viewportArea.isEmpty) return outer

        // The spacer row belongs to the content, so it shows at rest and is reclaimed on scrolling.
        Viewport(content.block(Block(padding = Padding(top = SPACER_ROWS)))).render(canvas.region(viewportArea), viewportState)
        val scroll = checkNotNull(viewportState.settled) { "a drawable viewport settles" }
        val thumbs = Style(role)
        Scrollbar.vertical(thumbs).render(canvas.region(Rect(canvas.width - 1, viewportArea.y, 1, viewportArea.height)), scroll)
        Scrollbar.horizontal(thumbs).render(canvas.region(Rect(viewportArea.x, canvas.height - 1, viewportArea.width, 1)), scroll)

        val contentBounds = Rect(
            viewportArea.x - scroll.offset.x,
            viewportArea.y - scroll.offset.y + SPACER_ROWS,
            scroll.content.width,
            (scroll.content.height - SPACER_ROWS).coerceAtLeast(0),
        )
        return outer.copy(scroll = scroll, contentBounds = contentBounds)
    }

    private companion object {
        /** Blank columns between the frame's side borders and the content. */
        const val FRAME_PADDING = 1

        /** Blank rows written above the content, under the title. */
        const val SPACER_ROWS = 1
    }
}
