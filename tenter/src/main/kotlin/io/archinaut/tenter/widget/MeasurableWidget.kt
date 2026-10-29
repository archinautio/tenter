// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect

/**
 * A [Widget] whose size follows from the content it writes for a width, so it can be stacked,
 * placed side by side, and scrolled. Place a child with [RowWriter.draw], never by calling its
 * [write], which would write into the parent's rows.
 */
public fun interface MeasurableWidget : Widget {
    /** Writes this widget's content from row 0; the written content may be painted repeatedly. */
    public fun write(writer: RowWriter)

    /** Writes this widget at the canvas width and paints it; outside a [Viewport], its visibility requests are dropped. */
    public override fun render(canvas: Canvas) {
        prepare(canvas.width).render(canvas)
    }
}

/** This plain widget at a fixed [width] and [height], invoked on each paint; it makes no visibility request of its own. */
public fun Widget.fixedSize(width: Int, height: Int): MeasurableWidget {
    require(width >= 0) { "content width must not be negative: $width" }
    require(height >= 0) { "content height must not be negative: $height" }
    val widget = this
    return object : LayoutWidget {
        override fun layout(availableWidth: Int): ContentLayout = ContentLayout.fixed(width, height, widget)
    }
}

/** This widget asking an enclosing [Viewport] to keep all of it visible, overriding requests made inside it. */
public fun MeasurableWidget.revealed(): MeasurableWidget =
    Requesting(this) { VisibilityRequest.Reveal(Rect(0, 0, it.width, it.height)) }

/** This widget asking an enclosing [Viewport] to keep [area], in its own coordinates, visible. */
public fun MeasurableWidget.revealed(area: Rect): MeasurableWidget = Requesting(this) { VisibilityRequest.Reveal(area) }

/** This widget asking an enclosing [Viewport] to keep its last row visible as it grows, while the reader is at the end. */
public fun MeasurableWidget.followingEnd(): MeasurableWidget =
    Requesting(this) { VisibilityRequest.End(Rect(0, it.height - 1, it.width, 1)) }

private class Requesting(
    private val content: MeasurableWidget,
    private val request: (ContentLayout) -> VisibilityRequest,
) : LayoutWidget {
    override fun layout(availableWidth: Int): ContentLayout {
        val child = content.prepare(availableWidth)
        return contentLayout(child.width, child.height) {
            place(0, 0, child)
            request(request(child))
        }
    }
}

/** A measurable widget that places prepared children at explicit coordinates instead of writing rows. */
internal interface LayoutWidget : MeasurableWidget {
    /** Lays out this widget for [availableWidth], which [prepare] has checked is not negative. */
    fun layout(availableWidth: Int): ContentLayout

    override fun write(writer: RowWriter) {
        writer.draw(this)
    }
}

/** Records this widget's content for [availableWidth]. */
internal fun MeasurableWidget.prepare(availableWidth: Int): ContentLayout {
    require(availableWidth >= 0) { "available width must not be negative: $availableWidth" }
    if (this is LayoutWidget) return layout(availableWidth)
    return RowWriter(availableWidth).also(::write).build()
}

internal fun contentLayout(
    width: Int,
    height: Int,
    block: ContentLayout.Builder.() -> Unit,
): ContentLayout = ContentLayout.Builder().apply(block).build(width, height)
