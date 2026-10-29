// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

/**
 * What one [MeasurableWidget] recorded at one width: its logical size, text, children, painted
 * widgets (invoked on each paint), and visibility requests, of which the last one wins.
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

    class Builder {
        private val instructions = mutableListOf<Instruction>()

        /** Writes [text] at ([x], [y]); anything past the layout's edge is clipped. */
        fun text(x: Int, y: Int, text: String, style: Style) {
            instructions += Instruction.Text(x, y, text, style)
        }

        /** Places [content] at ([x], [y]). */
        fun place(x: Int, y: Int, content: ContentLayout) {
            instructions += Instruction.Child(x, y, content)
        }

        fun request(request: VisibilityRequest) {
            instructions += Instruction.Request(request)
        }

        /** Paints [widget] over this layout's whole area, in place. */
        fun paint(widget: Widget) {
            instructions += Instruction.Paint(widget)
        }

        /** A [width] by [height] layout of what was recorded so far. */
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

    /** Paints this layout from the canvas's top-left corner, following no request. */
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

    /** Paints into [canvas], whose area is this layout's, skipping text and children it cannot show. */
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
