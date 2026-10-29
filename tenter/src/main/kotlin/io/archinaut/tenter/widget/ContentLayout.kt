// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

/**
 * Recorded dimensions, text, placements, and visibility requests of one [MeasurableWidget] at one
 * width. Dimensions describe logical content, not the number of nonblank cells painted. The
 * instruction list is immutable, but painted widgets (decorations, and plain widgets from
 * [fixedSize]) are retained and invoked on each paint. When several visibility requests are
 * recorded, the last one wins.
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

    /** Records instructions for a layout; children are already prepared. */
    class Builder {
        private val instructions = mutableListOf<Instruction>()

        /** Writes [text] at layout coordinate ([x], [y]); anything past the layout's edge is clipped. */
        fun text(x: Int, y: Int, text: String, style: Style) {
            instructions += Instruction.Text(x, y, text, style)
        }

        /** Places [content] at the declared layout coordinate ([x], [y]). */
        fun place(x: Int, y: Int, content: ContentLayout) {
            instructions += Instruction.Child(x, y, content)
        }

        /** Asks an enclosing [Viewport] to keep [request]'s layout-local area visible. */
        fun request(request: VisibilityRequest) {
            instructions += Instruction.Request(request)
        }

        /** Paints [widget] over this layout's whole area, in place. */
        fun paint(widget: Widget) {
            instructions += Instruction.Paint(widget)
        }

        /** A [width] by [height] layout of what was recorded so far; later operations do not change it. */
        fun build(width: Int, height: Int): ContentLayout = ContentLayout(width, height, instructions.toList())
    }

    /**
     * This layout grown to [width] by [height], which must not be smaller: painted widgets stretch
     * to the new area, while children keep the size they were prepared at.
     */
    fun stretchedTo(width: Int, height: Int): ContentLayout {
        require(width >= this.width && height >= this.height) {
            "a ${this.width}x${this.height} layout cannot stretch to ${width}x$height"
        }
        if (width == this.width && height == this.height) return this
        return ContentLayout(width, height, instructions)
    }

    /** Paints this layout from the canvas's top-left corner. A plain paint follows no request. */
    override fun render(canvas: Canvas) {
        paint(canvas.window(Rect(0, 0, width, height)))
    }

    /** The winning visibility request among this layout's own and its children's, clipped to its bounds. */
    fun request(): VisibilityRequest? {
        var winner: VisibilityRequest? = null
        for (instruction in instructions) {
            val request = when (instruction) {
                is Instruction.Request -> instruction.request.clippedTo(width, height)
                is Instruction.Child -> instruction.content.request()
                    ?.movedBy(instruction.x, instruction.y)
                    ?.clippedTo(width, height)
                is Instruction.Text, is Instruction.Paint -> null
            }
            if (request != null) winner = request
        }
        return winner
    }

    /**
     * Paints into [canvas], whose area is this layout's, skipping text rows and children outside
     * the part [canvas] can show. Children paint straight into a window of the same buffer, so
     * composition costs no intermediate copy.
     */
    fun paint(canvas: Canvas) {
        val visible = canvas.visibleArea
        if (visible.isEmpty) return
        for (instruction in instructions) {
            when (instruction) {
                is Instruction.Text -> {
                    if (instruction.y >= visible.y && instruction.y < visible.y + visible.height) {
                        canvas.setString(instruction.x, instruction.y, instruction.text, instruction.style)
                    }
                }
                is Instruction.Request -> Unit
                is Instruction.Paint -> instruction.widget.render(canvas)
                is Instruction.Child -> {
                    val child = instruction.content
                    val rect = Rect(instruction.x, instruction.y, child.width, child.height)
                    if (!rect.isEmpty && !rect.clipTo(visible).isEmpty) {
                        child.paint(canvas.window(rect))
                    }
                }
            }
        }
    }

    companion object {
        /** A [width] by [height] layout that paints [widget] over its whole area at that size. */
        fun fixed(width: Int, height: Int, widget: Widget): ContentLayout {
            val painted = ContentLayout(width, height, listOf(Instruction.Paint(widget)))
            // Nested, so a stretched parent keeps the widget at the size it was given.
            return ContentLayout(width, height, listOf(Instruction.Child(0, 0, painted)))
        }
    }

    sealed interface Instruction {
        data class Text(
            val x: Int,
            val y: Int,
            val text: String,
            val style: Style,
        ) : Instruction

        data class Request(val request: VisibilityRequest) : Instruction

        data class Child(val x: Int, val y: Int, val content: ContentLayout) : Instruction

        data class Paint(val widget: Widget) : Instruction
    }
}
