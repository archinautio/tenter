// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size

/**
 * Mutable scrolling intent and the last immutable frame observation for one viewport. Commands
 * are queued until a drawable frame; they never rewrite [settled]. A state belongs to one
 * viewport and is confined to the application's rendering context.
 */
public class ViewportState(initialOffset: Point = Point.ORIGIN) {

    /**
     * Every value a frame carries forward, in one immutable record. [fork] and [adopt] copy this
     * single reference rather than field-by-field, so a value added here cannot be forgotten by
     * one of them and go stale across a managed frame.
     */
    private data class Carried(
        public val requestedOffset: Point,
        public val pendingRecenter: Boolean = false,
        public val settled: ScrollState? = null,
    )

    private var carried: Carried = Carried(initialOffset)

    /** The last completed drawable frame, or `null` before the first such frame. */
    public val settled: ScrollState? get() = carried.settled

    /** Stages a managed frame without consuming the owner's queued intent. */
    internal fun fork(): ViewportState = ViewportState().also { it.carried = carried }

    internal fun adopt(candidate: ViewportState) {
        carried = candidate.carried
    }

    /** Queues a relative scroll request for the next drawable frame. */
    public fun scrollBy(dx: Int, dy: Int) {
        carried = carried.copy(
            requestedOffset = Point(
                saturatingAdd(carried.requestedOffset.x, dx),
                saturatingAdd(carried.requestedOffset.y, dy),
            ),
        )
    }

    /** Queues a one-shot request to center the current reveal target on the next frame. */
    public fun requestRecenter() {
        carried = carried.copy(pendingRecenter = true)
    }

    internal fun requestedOffset(): Point = carried.requestedOffset

    internal fun shouldFollow(reveal: Rect?, viewport: Size): Boolean {
        if (reveal == null) return false
        val previous = carried.settled ?: return true
        return reveal != previous.revealed || viewport != previous.viewport
    }

    internal fun recenterRequested(): Boolean = carried.pendingRecenter

    internal fun settle(snapshot: ScrollState) {
        carried = Carried(
            requestedOffset = snapshot.offset,
            pendingRecenter = false,
            settled = snapshot,
        )
    }

    private fun saturatingAdd(left: Int, right: Int): Int =
        left.toLong().plus(right.toLong()).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
