// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size

/**
 * A [Widget] that can measure its height for a width before painting, so [Stack], [Columns], and
 * [Viewport] accept it. A plain [Widget] paints into whatever area it is given; a measurable one
 * writes its rows, and its height follows from what it wrote.
 *
 * Implement [write], or create one from a lambda: `MeasurableWidget { writer -> ... }`. Place a
 * child inside another measurable widget with [RowWriter.draw]; never call [write] yourself, as
 * the child would write into the parent's rows instead of its own.
 */
public fun interface MeasurableWidget : Widget {
    /**
     * Writes this widget's content. [RowWriter.width] is the available width and row 0 is this
     * widget's top. Capture frame data here; the written content may be painted repeatedly.
     */
    public fun write(writer: RowWriter)

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
 * This widget asking an enclosing [Viewport] to keep all of it visible, as a selected row does.
 * Its request comes after any made inside it, so it wins over them; as with every request, a
 * later sibling's still wins over it.
 */
public fun MeasurableWidget.revealed(): MeasurableWidget {
    val content = this
    return object : LayoutWidget {
        override fun layout(availableWidth: Int): ContentLayout {
            val child = content.prepare(availableWidth)
            return contentLayout(child.width, child.height) {
                place(0, 0, child)
                reveal(Rect(0, 0, child.width, child.height))
            }
        }
    }
}

/**
 * The internal seam behind [MeasurableWidget] for the library's own composites ([Stack],
 * [Columns], [block]), which place prepared children at explicit coordinates instead of writing
 * rows through a [RowWriter].
 */
internal interface LayoutWidget : MeasurableWidget {
    /** Lays out this widget for [availableWidth], which [prepare] has already checked is not negative. */
    fun layout(availableWidth: Int): ContentLayout

    override fun write(writer: RowWriter) {
        writer.draw(this)
    }
}

/** Records this widget's content for [availableWidth]; the single entry point for every measurable widget. */
internal fun MeasurableWidget.prepare(availableWidth: Int): ContentLayout {
    require(availableWidth >= 0) { "available width must not be negative: $availableWidth" }
    if (this is LayoutWidget) return layout(availableWidth)
    return RowWriter(availableWidth).also(::write).build()
}

/** Builds a fixed-size prepared composition from placement instructions. */
internal fun contentLayout(
    width: Int,
    height: Int,
    block: ContentLayout.Builder.() -> Unit,
): ContentLayout = ContentLayout.Builder().apply(block).build(width, height)
