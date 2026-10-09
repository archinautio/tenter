// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.palette.RolePalette
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

import com.github.ajalt.mordant.rendering.Size
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

/**
 * Draws frames to a Mordant [backend], in the manner of Ratatui's `Terminal`.
 *
 * The terminal owns two [Buffer]s. Each [draw] resets the current one, hands it to the caller
 * as a [Frame], sends only the cells that changed since the previous frame, and then swaps the
 * two buffers. A full repaint on every keystroke (a cursor nudge, a panel toggle) is otherwise
 * tens of KB on a large terminal. The very first draw, and any draw after a resize or [clear],
 * has no previous frame to diff against and falls back to a full repaint.
 *
 * [draw] follows [backend]'s size: whenever Mordant reports a new size (for example after
 * [resizeEvents] polls it), the next draw resizes both buffers. [resize] sets a size explicitly,
 * which holds until the backend's size next changes.
 *
 * [palette] resolves every [ColorRole] a caller draws with to a concrete color; picking *which*
 * palette to use (a light/dark choice, detecting the terminal's color support, letting the user
 * override it) is the host application's job, not this terminal's — construct the [RolePalette]
 * before handing it here. The backend's own [com.github.ajalt.mordant.rendering.AnsiLevel] is
 * still read directly off [backend] for exactly one purpose: at
 * [com.github.ajalt.mordant.rendering.AnsiLevel.NONE] every SGR tag is suppressed regardless of
 * [palette], since the terminal cannot render color at all.
 *
 * The alternate screen and cursor lifecycle belongs to [withScreen]; this class only paints and
 * invalidates frames. It is not thread-safe.
 */
public class Terminal(private val backend: MordantTerminal, palette: RolePalette) {

    private val styleTagCache = StyleTagCache(palette, backend.terminalInfo.ansiLevel)

    private var backendSize: Size = backend.size
    private var current: Buffer = Buffer(backendSize.width.coerceAtLeast(0), backendSize.height.coerceAtLeast(0))

    // The buffer last sent to the backend, or null if nothing has been sent yet (or [clear] just
    // ran). It may have a different size than [current] right after a resize.
    private var previous: Buffer? = null

    /** The full drawable area of the next frame. */
    public val area: Rect get() = Rect(0, 0, current.width, current.height)

    /**
     * Resizes the drawing area to [width] x [height]. The next [draw] repaints fully, clearing
     * stale content first when the area shrank.
     */
    public fun resize(width: Int, height: Int) {
        require(width >= 0) { "terminal width must not be negative: $width" }
        require(height >= 0) { "terminal height must not be negative: $height" }
        if (width == current.width && height == current.height) return
        current = Buffer(width, height)
    }

    /**
     * Calls [render] with a fresh [Frame] covering [area], then sends what changed since the
     * previous frame. If [render] throws, nothing is sent, the previous frame remains the diff
     * base, and the exception propagates.
     *
     * The returned [CompletedFrame] reads the drawn cells and stays valid until the next draw.
     */
    public fun draw(render: (Frame) -> Unit): CompletedFrame {
        autoresize()
        val buffer = current
        buffer.reset()
        render(Frame(buffer))
        flush(buffer)
        val sent = previous
        current = if (sent != null && sent.width == buffer.width && sent.height == buffer.height) {
            sent
        } else {
            Buffer(buffer.width, buffer.height)
        }
        previous = buffer
        return CompletedFrame(buffer)
    }

    /**
     * Clears the current drawing destination and invalidates the diff base, so the next [draw]
     * repaints fully. The scoped [withScreen] operation owns alternate-screen and cursor
     * lifecycle; calling this method directly never starts or ends an alternate-screen session.
     *
     * The default style's SGR tag is emitted before `clearScreen()` (which erases using whatever
     * SGR is currently active) so the cleared screen uses the palette's background immediately.
     */
    public fun clear() {
        val defaultStyle = styleTagCache.tagsFor(Style.DEFAULT)?.open.orEmpty()
        backend.rawPrint(defaultStyle + backend.cursor.getMoves { clearScreen(); setPosition(0, 0) })
        previous = null
    }

    private fun autoresize() {
        // Mordant's Size compares by identity and the backend refreshes it on every print, so
        // only a change in dimensions counts as a resize.
        val size = backend.size
        if (size.width == backendSize.width && size.height == backendSize.height) return
        backendSize = size
        resize(size.width.coerceAtLeast(0), size.height.coerceAtLeast(0))
    }

    private fun flush(buffer: Buffer) {
        val prev = previous
        if (prev == null || prev.width != buffer.width || prev.height != buffer.height) {
            renderFull(buffer, clearStale = prev != null && (buffer.width < prev.width || buffer.height < prev.height))
        } else {
            renderDiff(buffer, prev)
        }
    }

    private fun renderFull(buffer: Buffer, clearStale: Boolean) {
        val sb = StringBuilder()
        sb.append(backend.cursor.getMoves {
            if (clearStale) clearScreen()
            setPosition(0, 0)
        })
        for (y in 0 until buffer.height) {
            renderSpan(sb, buffer, y, 0, buffer.width)
            if (y < buffer.height - 1) sb.append("\r\n")
        }
        backend.rawPrint(sb)
    }

    /**
     * Emits only the cells where [buffer] differs from [prev] (which must be the same size),
     * as one or more `setPosition` + styled-span writes. Emits nothing at all if the two buffers
     * are identical.
     */
    private fun renderDiff(buffer: Buffer, prev: Buffer) {
        val sb = StringBuilder()
        for (y in 0 until buffer.height) {
            val dirty = mutableListOf<IntRange>()
            for (x in 0 until buffer.width) {
                if (buffer.get(x, y) == prev.get(x, y)) continue
                val start = minOf(buffer.glyphStart(x, y), prev.glyphStart(x, y))
                val end = maxOf(buffer.glyphEndExclusive(x, y), prev.glyphEndExclusive(x, y))
                val last = dirty.lastOrNull()
                if (last != null && start <= last.last + 1) {
                    dirty[dirty.lastIndex] = last.first..maxOf(last.last, end - 1)
                } else {
                    dirty += start..(end - 1)
                }
            }
            for (span in dirty) {
                sb.append(backend.cursor.getMoves { setPosition(span.first, y) })
                renderSpan(sb, buffer, y, span.first, span.last + 1)
            }
        }
        if (sb.isEmpty()) return
        backend.rawPrint(sb)
    }

    /**
     * Appends styled text for `buffer[xStart, xEnd)` on row [y] to [sb], grouping consecutive
     * same-style cells into runs and emitting only the ANSI tags needed at each run boundary.
     *
     * Closing the previous run before opening the next is skippable whenever both runs have tags
     * at all: every opening tag now establishes BOTH foreground and background explicitly (even
     * for [ChromeRole.DEFAULT], which resolves to the palette's default surface rather than leaving a
     * channel unset), so the next run's open tag always fully overwrites whatever the previous
     * run set — nothing can bleed through. Only a strikethrough-state change still needs the
     * close, since strikethrough has no "close" bundled into every open tag the way color does.
     */
    private fun renderSpan(sb: StringBuilder, buffer: Buffer, y: Int, xStart: Int, xEnd: Int) {
        var x = xStart
        var activeStyle: Style? = null
        var activeTags: StyleTagCache.Tags? = null
        while (x < xEnd) {
            val runStyle = buffer.get(x, y).style
            val runChars = StringBuilder()
            while (x < xEnd) {
                val cell: Cell = buffer.get(x, y)
                if (cell.style != runStyle) break
                runChars.append(cell.symbol)
                x++
            }
            val tags = styleTagCache.tagsFor(runStyle)
            if (tags == null) {
                activeTags?.let { sb.append(it.close) }
                activeStyle = null
                activeTags = null
            } else {
                val skipClose = activeStyle != null && activeStyle.strikethrough == runStyle.strikethrough
                if (!skipClose) activeTags?.let { sb.append(it.close) }
                sb.append(tags.open)
                activeStyle = runStyle
                activeTags = tags
            }
            sb.append(runChars)
        }
        activeTags?.let { sb.append(it.close) }
    }
}
