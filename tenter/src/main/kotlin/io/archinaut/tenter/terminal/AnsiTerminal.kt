// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.color.ColorScheme
import io.archinaut.tenter.color.ColorSpace
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Cell

import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Size
import kotlinx.coroutines.flow.Flow
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

/**
 * A [Terminal] that draws ANSI escape sequences to a Mordant [backend] and reads input and size
 * from it. Each session sends only the cells that changed since its previous frame, which would
 * otherwise be tens of KB per keystroke on a large terminal; the first frame and any frame after a
 * resize repaint fully.
 */
internal class AnsiTerminal(
    private val backend: MordantTerminal,
) : Terminal(backend.size.width, backend.size.height) {

    private var backendSize: Size = backend.size

    override val colorSpace: ColorSpace? = when (backend.terminalInfo.ansiLevel) {
        AnsiLevel.TRUECOLOR -> ColorSpace.TRUECOLOR
        AnsiLevel.ANSI256 -> ColorSpace.ANSI256
        AnsiLevel.ANSI16 -> ColorSpace.ANSI16
        AnsiLevel.NONE -> null
    }

    override fun followSize() {
        // Mordant's Size compares by identity and every `updateSize` poll replaces it, so only a
        // change in dimensions counts as a resize.
        val size = backend.size
        if (size.width == backendSize.width && size.height == backendSize.height) return
        backendSize = size
        resize(size.width.coerceAtLeast(0), size.height.coerceAtLeast(0))
    }

    override fun open(colorScheme: ColorScheme): Session {
        val lifecycle = ScreenLifecycle(backend)
        val styles = if (colorSpace == null) NoStyleTags else StyleTagCache(colorScheme)
        val session = AnsiSession(styles, lifecycle)
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
        private val styles: StyleTags,
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

        /** Clears the screen with the color scheme's background, since `clearScreen()` erases with the active SGR. */
        fun clear() {
            val defaultStyle = styles.tagsFor(Cell.EMPTY).open
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

        /** Emits only the cells where [buffer] differs from the same-sized [prev]. */
        private fun renderDiff(buffer: Buffer, prev: Buffer) {
            val sb = StringBuilder()
            for (y in 0 until buffer.height) {
                // The dirty span still growing, `[spanStart, spanEnd)`; empty while spanStart < 0.
                var spanStart = -1
                var spanEnd = -1
                for (x in 0 until buffer.width) {
                    if (buffer.get(x, y) == prev.get(x, y)) continue
                    val start = minOf(buffer.glyphStart(x, y), prev.glyphStart(x, y))
                    val end = maxOf(buffer.glyphEndExclusive(x, y), prev.glyphEndExclusive(x, y))
                    if (spanStart >= 0 && start <= spanEnd) {
                        spanEnd = maxOf(spanEnd, end)
                    } else {
                        if (spanStart >= 0) renderDirtySpan(sb, buffer, y, spanStart, spanEnd)
                        spanStart = start
                        spanEnd = end
                    }
                }
                if (spanStart >= 0) renderDirtySpan(sb, buffer, y, spanStart, spanEnd)
            }
            if (sb.isEmpty()) return
            backend.rawPrint(sb)
        }

        private fun renderDirtySpan(sb: StringBuilder, buffer: Buffer, y: Int, xStart: Int, xEnd: Int) {
            sb.append(backend.cursor.getMoves { setPosition(xStart, y) })
            renderSpan(sb, buffer, y, xStart, xEnd)
        }

        /**
         * Appends `buffer[xStart, xEnd)` on row [y] to [sb] as same-style runs. A run is closed
         * before the next only when attributes change: every opening tag sets both colors
         * explicitly, so colors cannot bleed from one run into the next.
         */
        private fun renderSpan(sb: StringBuilder, buffer: Buffer, y: Int, xStart: Int, xEnd: Int) {
            var x = xStart
            // No run is open yet: NONE closes nothing, so the starting attributes never affect the output.
            var activeAttributes = Cell.EMPTY.attributes
            var activeTags: Tags = Tags.NONE
            while (x < xEnd) {
                val lead = buffer.get(x, y)
                val tags = styles.tagsFor(lead)
                if (activeAttributes != lead.attributes) sb.append(activeTags.close)
                sb.append(tags.open)
                activeAttributes = lead.attributes
                activeTags = tags
                while (x < xEnd) {
                    val cell: Cell = buffer.get(x, y)
                    if (!cell.looksLike(lead)) break
                    sb.append(cell.symbol)
                    x++
                }
            }
            sb.append(activeTags.close)
        }
    }
}

/** The [StyleTags] of a terminal that shows no color: no style emits an escape. */
private object NoStyleTags : StyleTags {
    override fun tagsFor(cell: Cell): Tags = Tags.NONE
}
