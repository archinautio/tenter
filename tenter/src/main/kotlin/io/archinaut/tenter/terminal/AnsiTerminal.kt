// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.palette.RolePalette
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Style

import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Size
import kotlinx.coroutines.flow.Flow
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

/**
 * A [Terminal] that draws frames as ANSI escape sequences to a Mordant [backend], and reads its
 * input and size from it.
 *
 * Each session sends only the cells that changed since its previous frame, keeping the last
 * presented buffer as the diff base and recycling the one before it for the next draw. A full
 * repaint on every keystroke (a cursor nudge, a panel toggle) is otherwise tens of KB on a large
 * terminal. A session's first frame, and any frame after a resize, has no previous frame of its
 * size to diff against and falls back to a full repaint.
 *
 * The drawing area follows [backend]'s size: whenever Mordant reports a new size (for example after
 * [events] polls it), the next draw resizes. At [AnsiLevel.NONE] every SGR tag is suppressed
 * regardless of the palette, since the terminal cannot render color at all.
 */
internal class AnsiTerminal(
    private val backend: MordantTerminal,
) : Terminal(backend.size.width, backend.size.height) {

    private var backendSize: Size = backend.size

    override val ansiLevel: AnsiLevel get() = backend.terminalInfo.ansiLevel

    override fun followSize() {
        // Mordant's Size compares by identity and the backend refreshes it on every print, so
        // only a change in dimensions counts as a resize.
        val size = backend.size
        if (size.width == backendSize.width && size.height == backendSize.height) return
        backendSize = size
        resize(size.width.coerceAtLeast(0), size.height.coerceAtLeast(0))
    }

    override fun open(palette: RolePalette): Session {
        val lifecycle = ScreenLifecycle(backend)
        val session = AnsiSession(StyleTagCache(palette, ansiLevel), lifecycle)
        try {
            lifecycle.enter()
            session.clear()
        } catch (thrown: Throwable) {
            try {
                lifecycle.close()
            } catch (cleanupFailure: Throwable) {
                thrown.addSuppressed(cleanupFailure)
            }
            throw thrown
        }
        return session
    }

    override fun events(mouseTracking: MouseTracking): Flow<TerminalEvent> = backend.events(mouseTracking)

    private inner class AnsiSession(
        private val styles: StyleTagCache,
        private val lifecycle: ScreenLifecycle,
    ) : Session {

        // The buffer last sent to the backend, or null if nothing has been sent since entry. It may
        // have a different size than the next frame right after a resize.
        private var previous: Buffer? = null

        // The buffer sent before [previous], reused for the next frame when the size still fits.
        private var spare: Buffer? = null

        override fun nextBuffer(width: Int, height: Int): Buffer {
            val recycled = spare
            if (recycled == null || recycled.width != width || recycled.height != height) return Buffer(width, height)
            recycled.reset()
            return recycled
        }

        override fun present(buffer: Buffer): CompletedFrame {
            flush(buffer)
            spare = previous
            previous = buffer
            return CompletedFrame(buffer)
        }

        override fun close() {
            lifecycle.close()
        }

        /**
         * Clears the screen so the next frame repaints fully. The default style's SGR tag is emitted
         * before `clearScreen()` (which erases using whatever SGR is currently active) so the cleared
         * screen uses the palette's background immediately.
         */
        fun clear() {
            val defaultStyle = styles.tagsFor(Style.DEFAULT)?.open.orEmpty()
            backend.rawPrint(defaultStyle + backend.cursor.getMoves { clearScreen(); setPosition(0, 0) })
            previous = null
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
         * for [ChromeRole.DEFAULT], which resolves to the palette's default surface rather than leaving
         * a channel unset), so the next run's open tag always fully overwrites whatever the previous
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
                val tags = styles.tagsFor(runStyle)
                if (tags == null) {
                    activeTags?.let { sb.append(it.close) }
                    activeStyle = null
                    activeTags = null
                } else {
                    val skipClose = activeStyle != null &&
                        (activeStyle.strikethrough ?: false) == (runStyle.strikethrough ?: false)
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
}
