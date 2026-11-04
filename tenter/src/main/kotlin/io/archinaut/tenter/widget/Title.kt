// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import com.github.ajalt.mordant.rendering.TextAlign
import io.archinaut.tenter.screen.Line

/**
 * A [Block]'s title: [line] and an optional [badge], such as the key that focuses a panel, placed
 * along the top row by [align]. A channel a span of [line] leaves unset takes the block's title
 * color; one it sets keeps its own, whatever the block's focus.
 */
public class Title(
    internal val line: Line,
    internal val badge: String? = null,
    internal val align: TextAlign = TextAlign.LEFT,
) {
    /** [text] as a one-span title. */
    public constructor(text: String, badge: String? = null, align: TextAlign = TextAlign.LEFT) :
        this(Line(text), badge, align)

    /** True when there is neither text nor a badge to show, so the title takes no row. */
    internal val isEmpty: Boolean get() = line.spans.isEmpty() && badge == null
}
