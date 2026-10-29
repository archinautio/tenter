// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Size

/**
 * One viewport's queued scrolling and its last completed frame. It chooses where each frame
 * scrolls to: the queued offset, unless the content's visibility request is followed this frame.
 * Commands wait for a drawable frame and never change [settled].
 */
public class ViewportState {

    private var requestedOffset: Point = Point.ORIGIN

    /** A queued placement for the next frame, overriding the request's own follow rule. */
    private var pendingPlacement: Placement? = null

    /** The last completed drawable frame, or `null` before the first such frame. */
    public var settled: ScrollState? = null
        private set

    /** Queues scrolling by ([dx], [dy]) cells. */
    public fun scrollBy(dx: Int, dy: Int) {
        requestedOffset = Point(
            saturatingAdd(requestedOffset.x, dx),
            saturatingAdd(requestedOffset.y, dy),
        )
    }

    /** Queues centering the content's visibility request on the next drawable frame. */
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

    /** Records [snapshot] as the completed frame. */
    internal fun settle(snapshot: ScrollState) {
        requestedOffset = snapshot.offset
        pendingPlacement = null
        settled = snapshot
    }

    private fun saturatingAdd(left: Int, right: Int): Int =
        left.toLong().plus(right.toLong()).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
