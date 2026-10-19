// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.widget.ContentWidget
import io.archinaut.tenter.widget.ViewportState
import io.archinaut.tenter.widget.ScrollingPanel

/**
 * One stateful panel declaration. A panel owns its current state, restore state, attachment, and
 * viewport state; [PanelSet] is the only public module that may mutate those values. Each state
 * builder returns a [Presentation], keeping the prepared content and the width that belongs to it
 * together for one frame.
 *
 * Visibility is still the host application's decision. A declared builder must always return a
 * presentation, while a host chooses which panels to include in a [Panels] frame.
 */
public class Panel<K : PanelId, I>(
    internal val id: K,
    private val title: String,
    private val badge: String? = null,
    private val normal: (I) -> Presentation,
    private val minimized: ((I) -> Presentation)? = null,
    private val maximized: ((I) -> Presentation)? = null,
) {
    /** Prepared content and its sizing preference for one rendered state. */
    public class Presentation private constructor(
        public val content: ContentWidget,
        private val preferredWidth: Int?,
    ) {
        internal fun requiredWidth(): Int = requireNotNull(preferredWidth) {
            "A side, minimized, or configured fixed-width panel requires Presentation.fixedWidth"
        }

        public companion object {
            /** Uses the space assigned to a main, proportional, or maximized panel. */
            public fun allocated(content: ContentWidget): Presentation = Presentation(content, null)

            /**
             * Preferred outer width for a side, minimized, or configured fixed-width panel.
             * Clipped to available space. Main, proportional, and maximized slots always use
             * their allocated width, allowing a declaration to be reused across layout modes.
             */
            public fun fixedWidth(content: ContentWidget, width: Int): Presentation {
                require(width > 0) { "Panel presentation width must be positive: $width" }
                return Presentation(content, width)
            }
        }
    }

    private val viewportState: ViewportState = ViewportState()
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

    internal fun presentation(inputs: I): Presentation = when (mode) {
        PanelMode.MINIMIZED -> minimized ?: error("Panel $id is in MINIMIZED mode but declares no minimized presentation")
        PanelMode.NORMAL -> normal
        PanelMode.MAXIMIZED -> maximized ?: error("Panel $id is in MAXIMIZED mode but declares no maximized presentation")
    }.invoke(inputs)

    internal fun claimAttachment() {
        require(!attached) { "Panel $id is already attached to a PanelSet" }
        attached = true
    }

    internal fun requireUnattached() {
        require(!attached) { "Panel $id is already attached to a PanelSet" }
    }

    /**
     * Renders the already-selected [presentation] into [canvas]. Selection happens in
     * [PanelSet] so the application builder runs exactly once per rendered presentation.
     */
    internal fun render(
        canvas: Canvas,
        presentation: Presentation,
        focused: Boolean,
    ): ViewportState {
        val candidate = viewportState.fork()
        val role = if (focused) ChromeRole.PANEL_BORDER_FOCUSED else ChromeRole.PANEL_BORDER
        val scrollingPanel = ScrollingPanel(
            content = presentation.content,
            title = title,
            badge = badge,
            borderColor = role,
            titleColor = role,
        )
        scrollingPanel.render(canvas, candidate)
        return candidate
    }

    internal fun settle(candidate: ViewportState) {
        viewportState.adopt(candidate)
    }

    public companion object {
        /** Column width of a minimized panel when an application has no narrower preference. */
        public const val MINIMIZED_WIDTH: Int = 7
    }
}
