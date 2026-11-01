// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

/**
 * The cells a [Screen.draw] call presented, or a part of them taken with [region]. A real
 * terminal's reads its own buffer, so everything it answers, [equals] included, is only valid until
 * the next draw; a [HeadlessTerminal]'s stays valid for good.
 *
 * Two frames are equal when they have the same size and every cell shows the same symbol in the
 * same style as the terminal renders it: an unset color equals [io.archinaut.tenter.color.StandardRole.DEFAULT],
 * an unset attribute equals off. Where a frame sits on screen does not count, so a [region] can
 * equal a frame built with [of].
 */
public class CompletedFrame private constructor(
    private val buffer: Buffer,
    private val left: Int,
    private val top: Int,
    public val width: Int,
    public val height: Int,
) {

    internal constructor(buffer: Buffer) : this(buffer, 0, 0, buffer.width, buffer.height)

    /** This frame in its own coordinates: `Rect(0, 0, width, height)`. */
    public val area: Rect get() = Rect(0, 0, width, height)

    /** @throws IndexOutOfBoundsException if ([x], [y]) is outside [area]. */
    public fun get(x: Int, y: Int): Cell {
        if (!area.contains(x, y)) {
            throw IndexOutOfBoundsException("($x, $y) out of bounds for ${width}x$height frame")
        }
        return buffer.get(left + x, top + y)
    }

    /**
     * The part of this frame inside [rect], in its own coordinates, `(0, 0)` being [rect]'s top-left cell.
     *
     * @throws IndexOutOfBoundsException if [rect] is not inside [area].
     */
    public fun region(rect: Rect): CompletedFrame {
        if (rect.x < 0 || rect.y < 0 || rect.x + rect.width > width || rect.y + rect.height > height) {
            throw IndexOutOfBoundsException("$rect is not inside the ${width}x$height frame")
        }
        return CompletedFrame(buffer, left + rect.x, top + rect.y, rect.width, rect.height)
    }

    /** Row [y]'s text, right-trimmed. A wide character appears once, not once per column it covers. */
    public fun line(y: Int): String = rowText(y).trimEnd()

    /** Every row's text, right-trimmed, joined by `\n`, without the blank rows at the bottom. */
    public fun text(): String = (0 until height).map(::line).dropLastWhile { it.isEmpty() }.joinToString("\n")

    /** Where [text] first starts, scanning rows top to bottom, in this frame's cell coordinates; null if absent. */
    public fun find(text: String): Point? {
        for (y in 0 until height) {
            val symbols = Array(width) { x -> get(x, y).symbol }
            val index = symbols.joinToString("").indexOf(text)
            if (index < 0) continue
            // A symbol spans several chars, so walk them to the column holding the match's first char.
            var end = 0
            for (x in 0 until width) {
                end += symbols[x].length
                if (index < end) return Point(x, y)
            }
            return Point(width, y)
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompletedFrame || width != other.width || height != other.height) return false
        return positions().all { (x, y) -> sameLook(get(x, y), other.get(x, y)) }
    }

    override fun hashCode(): Int = positions().fold(31 * width + height) { hash, (x, y) ->
        val cell = get(x, y)
        (hash * 31 + cell.symbol.hashCode()) * 31 + cell.style.rendered().hashCode()
    }

    /** Each row quoted, then the rendered style wherever it changes, scanning rows top to bottom. */
    override fun toString(): String = buildString {
        append("CompletedFrame ${width}x$height\n")
        for (y in 0 until height) append("  \"").append(rowText(y)).append("\"\n")
        append("styles:\n")
        var last: Style? = null
        for ((x, y) in positions()) {
            val style = get(x, y).style.rendered()
            if (style != last) {
                append("  ($x,$y) ").append(describe(style)).append('\n')
                last = style
            }
        }
    }

    public companion object {
        /** A frame of [lines], one row each in [Style.DEFAULT], as wide as the widest; shorter rows end in blanks. */
        public fun of(vararg lines: String): CompletedFrame = of(*Array(lines.size) { Line(lines[it]) })

        /** A frame of [lines], one row each with their styles, as wide as the widest; shorter rows end in blanks. */
        public fun of(vararg lines: Line): CompletedFrame {
            val buffer = Buffer(lines.maxOfOrNull { it.width } ?: 0, lines.size)
            val canvas = Canvas.of(buffer)
            lines.forEachIndexed { y, line ->
                line.spans.fold(0) { x, span -> canvas.setString(x, y, span.text, span.style).x }
            }
            return CompletedFrame(buffer)
        }

        /** A [width] x [height] frame of blank cells, as a freshly cleared screen shows. */
        internal fun blank(width: Int, height: Int): CompletedFrame = CompletedFrame(Buffer(width, height))

        private fun sameLook(a: Cell, b: Cell): Boolean = a.symbol == b.symbol && a.style.rendered() == b.style.rendered()

        private fun describe(style: Style): String = buildString {
            append("fg=").append(style.fg).append(" bg=").append(style.bg)
            if (style.bold == true) append(" bold")
            if (style.dim == true) append(" dim")
            if (style.italic == true) append(" italic")
            if (style.underline == true) append(" underline")
            if (style.inverse == true) append(" inverse")
            if (style.strikethrough == true) append(" strikethrough")
        }
    }
}

// File-private extensions rather than private members, so they never hide a caller's own extension
// of the same name: Kotlin resolves a call to an inaccessible member before an extension.
private fun CompletedFrame.rowText(y: Int): String {
    if (y !in 0 until height) {
        throw IndexOutOfBoundsException("row $y out of bounds for ${width}x$height frame")
    }
    return (0 until width).joinToString("") { x -> get(x, y).symbol }
}

private fun CompletedFrame.positions(): Sequence<Pair<Int, Int>> =
    (0 until height).asSequence().flatMap { y -> (0 until width).asSequence().map { x -> x to y } }
