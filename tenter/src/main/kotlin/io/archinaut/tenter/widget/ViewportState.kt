// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Size

/**
 * Mutable scrolling intent and the last immutable frame observation for one viewport. Commands
 * are queued until a drawable frame; they never rewrite [settled]. A state belongs to one
 * viewport and is confined to the application's rendering context.
 *
 * The state chooses where each frame scrolls to: the queued offset, unless the content's
 * visibility request (see [revealed] and [followingEnd]) is to be followed this frame.
 */
public class ViewportState {

    private var requestedOffset: Point = Point.ORIGIN

    /** How the next frame places the content's request, overriding its own follow rule; `null` when nothing is queued. */
    private var pendingPlacement: Placement? = null

    /** The last completed drawable frame, or `null` before the first such frame. */
    public var settled: ScrollState? = null
        private set

    /** Queues a relative scroll request for the next drawable frame. */
    public fun scrollBy(dx: Int, dy: Int) {
        requestedOffset = Point(
            saturatingAdd(requestedOffset.x, dx),
            saturatingAdd(requestedOffset.y, dy),
        )
    }

    /** Queues a one-shot request to center the content's current visibility request on the next frame. */
    public fun requestRecenter() {
        pendingPlacement = Placement.CENTER
    }

    /** Where this frame scrolls to for content making [request], shown in [viewport]. */
    internal fun targets(request: VisibilityRequest?, viewport: Size): ScrollTargets {
        val keep = ScrollTargets.keep(requestedOffset)
        if (request == null) return keep
        val placement = pendingPlacement
            ?: Placement.FOLLOW.takeIf { request.isFollowed(settled, requestedOffset, viewport) }
            ?: return keep
        return request.targets(requestedOffset, placement)
    }

    /** Records [snapshot] as the completed frame; the next frame starts from its offset. */
    internal fun settle(snapshot: ScrollState) {
        requestedOffset = snapshot.offset
        pendingPlacement = null
        settled = snapshot
    }

    private fun saturatingAdd(left: Int, right: Int): Int =
        left.toLong().plus(right.toLong()).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
