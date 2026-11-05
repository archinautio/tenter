// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import com.github.ajalt.mordant.rendering.TextAlign
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Padding

/**
 * One of a [Block]'s titles: [line] on the [position] row, placed by [align], with [padding] blank
 * cells on its left and right. A channel a span of [line] leaves unset takes the block's title
 * color; one it sets keeps its own, whatever the block's focus.
 *
 * [padding] is kept when [line] is shortened, and only its left and right edges may be set.
 */
public class Title(
    internal val line: Line,
    internal val align: TextAlign = TextAlign.LEFT,
    internal val position: TitlePosition = TitlePosition.TOP,
    internal val padding: Padding = Padding.horizontal(1),
) {
    init {
        require(padding.top == 0 && padding.bottom == 0) { "a title's padding has only left and right edges: $padding" }
    }

    /** [text] as a one-span title. */
    public constructor(
        text: String,
        align: TextAlign = TextAlign.LEFT,
        position: TitlePosition = TitlePosition.TOP,
        padding: Padding = Padding.horizontal(1),
    ) : this(Line(text), align, position, padding)

    /** True when there is no text to show, so the title takes no row. */
    internal val isEmpty: Boolean get() = line.spans.isEmpty()
}

/** The [Block] row a [Title] sits on. */
public enum class TitlePosition { TOP, BOTTOM }
