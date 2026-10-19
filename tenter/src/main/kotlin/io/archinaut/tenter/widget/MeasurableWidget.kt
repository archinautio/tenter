// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Size

/**
 * A [Widget] that can measure its height for a width before painting, so [Stack], [Columns], and
 * [Viewport] accept it. A plain [Widget] paints into whatever area it is given; a measurable one
 * writes its rows, and its height follows from what it wrote.
 *
 * Implement [write], or create one from a lambda: `MeasurableWidget { cursor -> ... }`. Place a
 * child inside another measurable widget with [TextCursor.draw]; never call [write] yourself, as
 * the child would write into the parent's rows instead of its own.
 */
public fun interface MeasurableWidget : Widget {
    /**
     * Writes this widget's content. [TextCursor.width] is the available width and row 0 is this
     * widget's top. Capture frame data here; the written content may be painted repeatedly.
     */
    public fun write(cursor: TextCursor)

    /** Writes this widget at the canvas width, then paints it from the top-left corner. */
    public override fun render(canvas: Canvas) {
        prepare(canvas.width).render(canvas)
    }
}

/** This widget's size at [width]: its written height, and a width that may exceed [width] for fixed-size content. */
public fun MeasurableWidget.measure(width: Int): Size {
    val layout = prepare(width)
    return Size(layout.width, layout.height)
}

/**
 * This plain widget with a fixed [width] and [height], so it can be stacked and scrolled. The
 * widget is retained rather than painted during measurement; each paint invokes it once, so
 * capture stable frame data in it when repeatable painting is required.
 */
public fun Widget.fixedSize(width: Int, height: Int): MeasurableWidget {
    require(width >= 0) { "content width must not be negative: $width" }
    require(height >= 0) { "content height must not be negative: $height" }
    val widget = this
    return object : LayoutWidget {
        override fun layout(availableWidth: Int): ContentLayout = ContentLayout.raw(width, height, widget)
    }
}

/**
 * The internal seam behind [MeasurableWidget] for the library's own composites ([Stack],
 * [Columns], [block]), which place prepared children at explicit coordinates instead of writing
 * rows through a cursor.
 */
internal interface LayoutWidget : MeasurableWidget {
    fun layout(availableWidth: Int): ContentLayout

    override fun write(cursor: TextCursor) {
        cursor.draw(this)
    }
}

/** Records this widget's content for [availableWidth]; the single entry point for every measurable widget. */
internal fun MeasurableWidget.prepare(availableWidth: Int): ContentLayout {
    require(availableWidth >= 0) { "available width must not be negative: $availableWidth" }
    if (this is LayoutWidget) return layout(availableWidth)
    val sink = RecordingTextSink(availableWidth)
    val cursor = TextCursor(availableWidth, sink)
    write(cursor)
    return ContentLayout(availableWidth, cursor.occupiedHeight, sink.snapshot())
}

/** Builds a fixed-size prepared composition from placement instructions. */
internal fun contentLayout(
    width: Int,
    height: Int,
    block: ContentLayout.Builder.() -> Unit,
): ContentLayout {
    val builder = ContentLayout.Builder()
    builder.block()
    return ContentLayout(width, height, builder.snapshot())
}
