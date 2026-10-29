// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.animation

import io.archinaut.tenter.screen.Size
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import io.archinaut.tenter.widget.Widget

/**
 * The timeline of a finite set of animations, sampled at elapsed time; it owns no clock. Sampling
 * is pure, so any time may be sampled in any order. A clip is visible exactly while
 * `startAfter <= elapsed < startAfter + frameDuration * frameCount`. The clips are copied when it
 * is built, and invalid or non-finite times throw [IllegalArgumentException].
 */
public class AnimationPlayback<T>(clips: List<Clip<T>>) {
    private val timelines: List<Timeline<T>> = clips.map(::Timeline)

    /** An animation together with the opaque value presented alongside each visible frame. */
    public data class Clip<T>(
        public val animation: Animation,
        public val value: T,
        public val startAfter: Duration = ZERO,
    )

    /** A visible animation frame and its application-owned value. */
    public data class VisibleFrame<T>(
        public val value: T,
        public val size: Size,
        public val content: Widget,
    )

    /**
     * The visible frames at a sampled time, and [nextChangeIn], the delay until a clip starts,
     * changes frame, or ends, or null once every clip has finished.
     */
    public data class Sample<T>(
        public val frames: List<VisibleFrame<T>>,
        public val nextChangeIn: Duration?,
    )

    /** The clips visible at [elapsed]. */
    public fun sample(elapsed: Duration): Sample<T> {
        require(elapsed.isFinite() && elapsed >= ZERO) {
            "elapsed must be finite and non-negative, was $elapsed"
        }

        val frames = ArrayList<VisibleFrame<T>>()
        var nextChange: Duration? = null

        timelines.forEach { timeline ->
            when {
                elapsed < timeline.startAfter -> {
                    nextChange = minPositive(nextChange, timeline.startAfter - elapsed)
                }

                elapsed < timeline.endAt -> {
                    val frameIndex = timeline.frameIndexAt(elapsed)
                    frames += VisibleFrame(
                        value = timeline.value,
                        size = timeline.size,
                        content = timeline.animation.frame(frameIndex),
                    )
                    val transition = if (frameIndex + 1 < timeline.frameCount) {
                        timeline.startAfter + timeline.frameDuration * (frameIndex + 1)
                    } else {
                        timeline.endAt
                    }
                    nextChange = minPositive(nextChange, transition - elapsed)
                }
            }
        }

        return Sample(frames = frames.toList(), nextChangeIn = nextChange)
    }

    private class Timeline<T>(clip: Clip<T>) {
        internal val animation: Animation = clip.animation
        internal val value: T = clip.value
        internal val size: Size = animation.size
        internal val frameCount: Int = animation.frameCount
        internal val frameDuration: Duration = animation.frameDuration
        internal val startAfter: Duration = clip.startAfter
        internal val endAt: Duration

        init {
            require(size.width > 0 && size.height > 0) { "animation size must be positive, was $size" }
            require(frameCount > 0) { "frameCount must be positive, was $frameCount" }
            require(frameDuration.isFinite() && frameDuration > ZERO) {
                "frameDuration must be finite and positive, was $frameDuration"
            }
            require(startAfter.isFinite() && startAfter >= ZERO) {
                "startAfter must be finite and non-negative, was $startAfter"
            }
            val totalDuration = frameDuration * frameCount
            require(totalDuration.isFinite()) {
                "animation duration overflowed to infinity"
            }
            endAt = startAfter + totalDuration
            require(endAt.isFinite()) {
                "animation end offset overflowed to infinity"
            }
        }

        internal fun frameIndexAt(elapsed: Duration): Int {
            var low = 0
            var high = frameCount - 1
            var result = 0

            while (low <= high) {
                val midpoint = low + (high - low) / 2
                val frameStart = startAfter + frameDuration * midpoint
                if (frameStart <= elapsed) {
                    result = midpoint
                    low = midpoint + 1
                } else {
                    high = midpoint - 1
                }
            }
            return result
        }
    }

    private companion object {
        private fun minPositive(current: Duration?, candidate: Duration): Duration =
            if (current == null || candidate < current) candidate else current
    }
}
