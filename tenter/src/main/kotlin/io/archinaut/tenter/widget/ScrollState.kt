// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Rect

/**
 * An immutable observation of a completed viewport frame. [revealed] is in content coordinates;
 * dimensions are in cells and describe the exact measured content and destination used for the
 * frame.
 */
public data class ScrollState(
    public val offset: ScrollOffset,
    public val maxOffset: ScrollOffset,
    public val revealed: Rect? = null,
    public val viewportWidth: Int = 0,
    public val viewportHeight: Int = 0,
    public val contentWidth: Int = 0,
    public val contentHeight: Int = 0,
)
