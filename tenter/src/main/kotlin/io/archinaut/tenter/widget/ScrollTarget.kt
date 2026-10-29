// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Size

/**
 * Where a [Viewport] scrolls along one axis this frame: the one place "stay", "keep this
 * visible", and "center on this" turn into an offset, so no widget reimplements any of them.
 */
internal sealed interface ScrollTarget {
    /** The offset this target resolves to in a [viewportSize]-long window over content scrollable up to [maxOffset]. */
    fun offset(viewportSize: Int, maxOffset: Int): Int

    /** Stays at [offset], clamped to the content. */
    data class Keep(val offset: Int) : ScrollTarget {
        override fun offset(viewportSize: Int, maxOffset: Int): Int = offset.coerceIn(0, maxOffset.coerceAtLeast(0))
    }

    /**
     * The smallest move from [from] that brings `[start, end)` into view. An already-visible range
     * leaves [from] unchanged (clamped), so a manually panned view does not hunt back to a
     * canonical position. A range longer than the viewport aligns the edge it is reached from:
     * its [start] from below, its [end] from above.
     */
    data class Follow(val from: Int, val start: Int, val end: Int) : ScrollTarget {
        override fun offset(viewportSize: Int, maxOffset: Int): Int {
            if (maxOffset <= 0) return 0
            val clamped = from.coerceIn(0, maxOffset)
            val adjusted = when {
                start < clamped -> start
                end > clamped + viewportSize -> end - viewportSize
                else -> return clamped
            }
            return adjusted.coerceIn(0, maxOffset)
        }
    }

    /** The offset that centers `[start, end)` in the viewport. */
    data class Center(val start: Int, val end: Int) : ScrollTarget {
        override fun offset(viewportSize: Int, maxOffset: Int): Int {
            if (maxOffset <= 0) return 0
            val centered = start - (viewportSize - (end - start)) / 2
            return centered.coerceIn(0, maxOffset)
        }
    }
}

/** How a followed request is brought into view along one axis, from the offset [ScrollTarget.Follow] would start at. */
internal fun interface Placement {
    fun target(from: Int, start: Int, end: Int): ScrollTarget

    companion object {
        /** The smallest move that shows the range. */
        val FOLLOW: Placement = Placement(ScrollTarget::Follow)

        /** The range in the middle of the viewport. */
        val CENTER: Placement = Placement { _, start, end -> ScrollTarget.Center(start, end) }
    }
}

/** One [ScrollTarget] per axis. */
internal data class ScrollTargets(val x: ScrollTarget, val y: ScrollTarget) {
    fun offset(viewport: Size, maxOffset: Point): Point =
        Point(x.offset(viewport.width, maxOffset.x), y.offset(viewport.height, maxOffset.y))

    companion object {
        fun keep(offset: Point): ScrollTargets = ScrollTargets(ScrollTarget.Keep(offset.x), ScrollTarget.Keep(offset.y))
    }
}
