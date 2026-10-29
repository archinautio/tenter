// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size

/**
 * An immutable observation of a completed viewport frame, in cells: the [offset] shown and the
 * largest one possible, the [viewport] it was shown in, the measured [content], and the reveal
 * area the content asked to keep visible, in content coordinates, whether or not this frame
 * followed it.
 */
public data class ScrollState(
    public val offset: Point,
    public val maxOffset: Point,
    public val viewport: Size,
    public val content: Size,
    public val revealed: Rect? = null,
)
