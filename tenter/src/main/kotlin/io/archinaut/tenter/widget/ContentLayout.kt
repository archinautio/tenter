// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import java.util.IdentityHashMap
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

/**
 * Recorded dimensions, text, and placements of one [MeasurableWidget] at one width. Dimensions
 * describe logical content, not the number of nonblank cells painted. The instruction list is
 * immutable, but raw widgets from [fixedSize] are retained and invoked on each paint. When several
 * reveal requests are recorded, the last one wins.
 */
internal class ContentLayout(
    val width: Int,
    val height: Int,
    private val instructions: List<Instruction>,
) : Widget {

    init {
        require(width >= 0) { "content width must not be negative: $width" }
        require(height >= 0) { "content height must not be negative: $height" }
    }

    /** Placement instructions for the library's composites; children are already prepared. */
    class Builder {
        private val instructions = mutableListOf<Instruction>()

        /** Places [content] at the declared layout coordinate ([x], [y]). */
        fun place(x: Int, y: Int, content: ContentLayout) {
            instructions += Instruction.Child(x, y, content)
        }

        /** Requests visibility of a layout-local rectangle. Empty rectangles are ignored. */
        fun reveal(rect: Rect) {
            instructions += Instruction.Reveal(rect)
        }

        /** Paints [widget] over this layout's whole area, in place; it makes no reveal request. */
        fun decorate(widget: Widget) {
            instructions += Instruction.Decoration(widget)
        }

        fun snapshot(): List<Instruction> = instructions.toList()
    }

    override fun render(canvas: Canvas) {
        canvas.clearReveal()
        paintAt(canvas) { Point(0, 0) }?.let(canvas::markReveal)
    }

    /**
     * Resolves this layout's reveal request, asks [origin] where the layout's top-left lands in
     * [canvas] given that request, then paints only the part of the layout [canvas] can show.
     * Returns the reveal request in this layout's own coordinates.
     *
     * Each raw widget is invoked once per call, even though resolving the request needs its paint:
     * the offscreen result of the reveal pass is reused by the paint pass.
     */
    fun paintAt(canvas: Canvas, origin: (reveal: Rect?) -> Point): Rect? {
        val rendered = IdentityHashMap<Instruction.Raw, Canvas>()
        val reveal = resolveReveal(rendered)
        val at = origin(reveal)
        paint(canvas.window(Rect(at.x, at.y, width, height)), rendered)
        return reveal
    }

    /** The winning visibility request among this layout's own and its children's, clipped to its bounds. */
    private fun resolveReveal(rendered: MutableMap<Instruction.Raw, Canvas>): Rect? {
        val requests = mutableListOf<Rect>()
        for (instruction in instructions) {
            when (instruction) {
                is Instruction.Text -> Unit
                is Instruction.Decoration -> Unit
                is Instruction.Reveal -> clip(instruction.rect, width, height)?.let(requests::add)
                is Instruction.Child -> {
                    val child = instruction.content
                    if (child.width > 0 && child.height > 0) {
                        child.resolveReveal(rendered)?.let {
                            translateAndClip(
                                it,
                                instruction.x,
                                instruction.y,
                                child.width,
                                child.height,
                                width,
                                height,
                            )?.let(requests::add)
                        }
                    }
                }
                is Instruction.Raw -> {
                    if (width > 0 && height > 0) {
                        val source = rendered.getOrPut(instruction) {
                            Canvas.offscreen(width, height).also(instruction.widget::render)
                        }
                        source.revealRect()?.let { clip(it, width, height)?.let(requests::add) }
                    }
                }
            }
        }
        return requests.lastOrNull()
    }

    /**
     * Paints into [canvas], whose area is this layout's, skipping text rows and children outside
     * the part [canvas] can show. Children paint straight into a window of the same buffer, so
     * composition costs no intermediate copy.
     */
    private fun paint(canvas: Canvas, rendered: Map<Instruction.Raw, Canvas>) {
        val visible = canvas.visibleArea
        if (visible.isEmpty) return
        for (instruction in instructions) {
            when (instruction) {
                is Instruction.Text -> {
                    if (instruction.y >= visible.y && instruction.y < visible.y + visible.height) {
                        canvas.setString(instruction.x, instruction.y, instruction.text, instruction.style)
                    }
                }
                is Instruction.Reveal -> Unit
                is Instruction.Decoration -> instruction.widget.render(canvas)
                is Instruction.Child -> {
                    val child = instruction.content
                    val rect = Rect(instruction.x, instruction.y, child.width, child.height)
                    if (!rect.isEmpty && !rect.clipTo(visible).isEmpty) {
                        child.paint(canvas.window(rect), rendered)
                    }
                }
                is Instruction.Raw -> {
                    if (width > 0 && height > 0) {
                        val source = rendered[instruction]
                            ?: Canvas.offscreen(width, height).also(instruction.widget::render)
                        canvas.blit(source)
                    }
                }
            }
        }
    }

    companion object {
        fun raw(width: Int, height: Int, widget: Widget): ContentLayout =
            ContentLayout(width, height, listOf(Instruction.Raw(widget)))
    }

    sealed interface Instruction {
        data class Text(
            val x: Int,
            val y: Int,
            val text: String,
            val style: Style,
        ) : Instruction

        data class Reveal(val rect: Rect) : Instruction

        data class Child(val x: Int, val y: Int, val content: ContentLayout) : Instruction

        data class Raw(val widget: Widget) : Instruction

        data class Decoration(val widget: Widget) : Instruction
    }

    private fun clip(rect: Rect, boundWidth: Int, boundHeight: Int): Rect? =
        rect.clipTo(Rect(0, 0, boundWidth, boundHeight)).takeUnless { it.isEmpty }

    private fun translateAndClip(
        rect: Rect,
        x: Int,
        y: Int,
        childWidth: Int,
        childHeight: Int,
        boundWidth: Int,
        boundHeight: Int,
    ): Rect? {
        val visible = clip(rect, childWidth, childHeight) ?: return null
        return clip(visible.offset(x, y), boundWidth, boundHeight)
    }
}
