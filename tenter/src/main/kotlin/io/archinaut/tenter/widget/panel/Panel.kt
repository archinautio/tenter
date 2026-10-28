// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import com.github.ajalt.mordant.rendering.BorderType
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.ViewportState
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.Viewport
import io.archinaut.tenter.widget.block

/**
 * One stateful panel declaration. A panel owns its current mode, restore mode, attachment, and
 * viewport state; [PanelSet] is the only public module that may mutate those values. Each mode's
 * builder returns the panel's content for one frame.
 *
 * A panel has no size: where it goes and how big it is in each mode is the host application's
 * decision, made every frame by the areas it passes to [Panels]. [borderType] picks the frame's
 * glyphs, as for [Block]. With [stickToBottom], the panel keeps its last row visible as its
 * content grows, as [ViewportState] describes.
 */
public class Panel<K : PanelId, I>(
    internal val id: K,
    private val title: String,
    private val badge: String? = null,
    private val normal: (I) -> MeasurableWidget,
    private val minimized: ((I) -> MeasurableWidget)? = null,
    private val maximized: ((I) -> MeasurableWidget)? = null,
    private val borderType: BorderType = BorderType.ROUNDED,
    stickToBottom: Boolean = false,
) {
    private val viewportState: ViewportState = ViewportState(stickToBottom = stickToBottom)
    private var restoreMode: PanelMode = PanelMode.NORMAL
    private var attached: Boolean = false

    internal var mode: PanelMode = PanelMode.NORMAL
        private set

    /** The declared states, smallest first — what the owning set cycles. */
    internal val modes: List<PanelMode> = listOfNotNull(
        minimized?.let { PanelMode.MINIMIZED },
        PanelMode.NORMAL,
        maximized?.let { PanelMode.MAXIMIZED },
    )

    /** Steps [delta] through declared states, wrapping while preserving the restore state. */
    internal fun cycleMode(delta: Int) {
        val index = modes.indexOf(mode)
        val next = modes[(index + delta).mod(modes.size)]
        if (next == PanelMode.MAXIMIZED && mode != PanelMode.MAXIMIZED) restoreMode = mode
        mode = next
    }

    /** Returns to the state this panel was maximized from. */
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

    /**
     * Renders the already-built [content] into [canvas]. Building happens in [PanelSet] so the
     * application builder runs exactly once per rendered frame.
     */
    internal fun render(
        canvas: Canvas,
        content: MeasurableWidget,
        focused: Boolean,
    ): ViewportState {
        val candidate = viewportState.fork()
        val role = if (focused) StandardRole.PANEL_BORDER_FOCUSED else StandardRole.PANEL_BORDER
        panelViewport(content, title, badge, borderColor = role, titleColor = role, borderType = borderType)
            .render(canvas, candidate)
        return candidate
    }

    internal fun settle(candidate: ViewportState) {
        viewportState.adopt(candidate)
    }
}

/**
 * The scrolling viewport every panel renders through: a bordered frame with horizontal padding
 * around [content]. The frame's padding is fixed, but the top spacer row is written into the
 * content stream, so it shows at rest and the content reclaims it the moment it scrolls.
 */
internal fun panelViewport(
    content: MeasurableWidget,
    title: String,
    badge: String?,
    borderColor: ColorRole = StandardRole.PANEL_BORDER,
    titleColor: ColorRole = StandardRole.ACCENT,
    borderType: BorderType = BorderType.ROUNDED,
): Viewport = Viewport(
    content.block(Block(padding = Block.PADDING.verticalEdges())),
    block = Block.bordered(
        title = title,
        badge = badge,
        padding = Block.PADDING.horizontalEdges(),
        borderColor = borderColor,
        titleColor = titleColor,
        borderType = borderType,
    ),
)
