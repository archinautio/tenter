// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size

/** Content asking an enclosing [Viewport] to keep [area] visible; each kind decides when and how it is followed. */
internal sealed class VisibilityRequest {
    abstract val area: Rect

    protected abstract fun at(area: Rect): VisibilityRequest

    /** Whether this frame scrolls to this request. */
    abstract fun isFollowed(previous: ScrollState?, requested: Point, viewport: Size): Boolean

    /** Where to scroll from [requested] to show this request as [placement] places it. */
    abstract fun targets(requested: Point, placement: Placement): ScrollTargets

    fun movedBy(dx: Int, dy: Int): VisibilityRequest = at(area.offset(dx, dy))

    /** This request clipped to a [width] by [height] area, or `null` when nothing is left. */
    fun clippedTo(width: Int, height: Int): VisibilityRequest? =
        area.clipTo(Rect(0, 0, width, height)).takeUnless { it.isEmpty }?.let(::at)

    /** A selection: followed whenever it moves or the viewport changes size. */
    data class Reveal(override val area: Rect) : VisibilityRequest() {
        override fun at(area: Rect): VisibilityRequest = Reveal(area)

        override fun isFollowed(previous: ScrollState?, requested: Point, viewport: Size): Boolean =
            previous == null || area != previous.revealed || viewport != previous.viewport

        override fun targets(requested: Point, placement: Placement): ScrollTargets = ScrollTargets(
            placement.target(requested.x, area.x, area.x + area.width),
            placement.target(requested.y, area.y, area.y + area.height),
        )
    }

    /** A log's last row: followed vertically while the reader still sees where the previous frame ended. */
    data class End(override val area: Rect) : VisibilityRequest() {
        override fun at(area: Rect): VisibilityRequest = End(area)

        override fun isFollowed(previous: ScrollState?, requested: Point, viewport: Size): Boolean {
            val previousEnd = previous?.revealed ?: return true
            val top = requested.y.coerceIn(0, previous.maxOffset.y)
            return previousEnd.y >= top && previousEnd.y + previousEnd.height <= top + previous.viewport.height
        }

        override fun targets(requested: Point, placement: Placement): ScrollTargets = ScrollTargets(
            ScrollTarget.Keep(requested.x),
            placement.target(requested.y, area.y, area.y + area.height),
        )
    }
}
