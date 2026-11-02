// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size

/**
 * A completed viewport frame, in cells: the [offset] shown and the largest possible, the [viewport]
 * and [content] sizes, and the area the content asked to keep visible, followed or not.
 */
public data class ScrollState(
    public val offset: Point,
    public val maxOffset: Point,
    public val viewport: Size,
    public val content: Size,
    public val revealed: Rect? = null,
)
