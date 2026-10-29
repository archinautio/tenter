// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.animation

import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.widget.Widget
import kotlin.time.Duration

/**
 * A finite sequence of equally timed frames. [size], [frameCount], and [frameDuration] are positive
 * and stable; [frame] is deterministic, draws within [size], and returns frames that stay stable.
 */
public interface Animation {
    public val size: Size
    public val frameCount: Int
    public val frameDuration: Duration

    /** Returns the content for [index], where [index] is in `0 until [frameCount]`. */
    public fun frame(index: Int): Widget
}
