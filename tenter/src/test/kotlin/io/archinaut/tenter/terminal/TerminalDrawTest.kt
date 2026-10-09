// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.palette.PaletteColor
import io.archinaut.tenter.palette.RolePalette
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.StatefulWidget
import io.archinaut.tenter.widget.Widget

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Size
import com.github.ajalt.mordant.terminal.TerminalInterface
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

private object PlainPalette : RolePalette {
    override val defaultBackground: PaletteColor = PaletteColor.TrueColor(0, 0, 0)

    override fun foreground(role: ColorRole): PaletteColor = PaletteColor.TrueColor(255, 255, 255)
}

/** A recorder whose reported size a test can change, like a user resizing a real terminal. */
private class ResizableRecorder(width: Int, height: Int) {
    val recorder = TerminalRecorder(ansiLevel = AnsiLevel.NONE, width = width, height = height, outputInteractive = true)
    var size: Size = Size(width, height)
    val backend = MordantTerminal(
        ansiLevel = AnsiLevel.NONE,
        terminalInterface = object : TerminalInterface by recorder {
            override fun getTerminalSize(): Size = size
        },
    )
}

private fun text(frame: CompletedFrame): String =
    (0 until frame.height).joinToString("\n") { y -> (0 until frame.width).joinToString("") { x -> frame.get(x, y).symbol } }

private class Label(private val text: String) : Widget {
    override fun render(canvas: Canvas) {
        canvas.setString(0, 0, text)
    }
}

internal class TerminalDrawTest {

    private val screen = ResizableRecorder(6, 2)
    private val terminal = Terminal(screen.backend, PlainPalette)

    @Test
    fun `the area starts at the backend's size`() {
        assertEquals(Rect(0, 0, 6, 2), terminal.area)
    }

    @Test
    fun `draw renders widgets into the frame and returns the completed cells`() {
        val completed = terminal.draw { frame ->
            assertEquals(Rect(0, 0, 6, 2), frame.area)
            frame.renderWidget(Label("hi"), Rect(1, 1, 5, 1))
        }

        assertEquals("      \n hi   ", text(completed))
        assertTrue(screen.recorder.output().contains("hi"))
    }

    @Test
    fun `an identical second frame sends nothing`() {
        terminal.draw { it.renderWidget(Label("same"), it.area) }
        screen.recorder.clearOutput()

        terminal.draw { it.renderWidget(Label("same"), it.area) }

        assertEquals("", screen.recorder.output())
    }

    @Test
    fun `each frame starts blank, so dropping a widget erases it`() {
        terminal.draw { it.renderWidget(Label("gone"), it.area) }
        screen.recorder.clearOutput()

        val completed = terminal.draw { }

        assertEquals("      \n      ", text(completed))
        assertTrue(screen.recorder.output().contains("    "), "expected the old cells to be blanked")
    }

    @Test
    fun `only the changed cells are sent`() {
        terminal.draw { it.renderWidget(Label("hello"), it.area) }
        screen.recorder.clearOutput()

        terminal.draw { it.renderWidget(Label("hallo"), it.area) }

        val out = screen.recorder.output()
        assertTrue(out.contains("a"), "expected the changed cell")
        assertTrue(!out.contains("llo"), "expected unchanged cells to be skipped: $out")
    }

    @Test
    fun `a failed draw sends nothing and keeps the previous frame as the diff base`() {
        terminal.draw { it.renderWidget(Label("kept"), it.area) }
        screen.recorder.clearOutput()
        val failure = IllegalStateException("render failed")

        val thrown = assertThrows<IllegalStateException> {
            terminal.draw { frame ->
                frame.renderWidget(Label("half"), frame.area)
                throw failure
            }
        }

        assertSame(failure, thrown)
        assertEquals("", screen.recorder.output())
        terminal.draw { it.renderWidget(Label("kept"), it.area) }
        assertEquals("", screen.recorder.output(), "the failed frame must not become the diff base")
    }

    @Test
    fun `draw follows a backend resize with a full repaint`() {
        terminal.draw { it.renderWidget(Label("a"), it.area) }
        screen.size = Size(4, 3)
        screen.backend.updateSize()
        screen.recorder.clearOutput()

        val completed = terminal.draw { it.renderWidget(Label("a"), it.area) }

        assertEquals(Rect(0, 0, 4, 3), completed.area)
        assertTrue(screen.recorder.output().contains("a"), "a resized frame must repaint fully")
    }

    @Test
    fun `an explicit resize holds until the backend size changes`() {
        terminal.resize(3, 1)

        assertEquals(Rect(0, 0, 3, 1), terminal.draw { }.area)
        assertEquals(Rect(0, 0, 3, 1), terminal.draw { }.area)

        screen.size = Size(5, 2)
        screen.backend.updateSize()

        assertEquals(Rect(0, 0, 5, 2), terminal.draw { }.area)
    }

    @Test
    fun `clear forces the next frame to repaint fully`() {
        terminal.draw { it.renderWidget(Label("x"), it.area) }
        terminal.clear()
        screen.recorder.clearOutput()

        terminal.draw { it.renderWidget(Label("x"), it.area) }

        assertTrue(screen.recorder.output().contains("x"))
    }

    @Test
    fun `stateful widgets receive the caller's state`() {
        val counter = object : StatefulWidget<IntArray> {
            override fun render(canvas: Canvas, state: IntArray) {
                state[0]++
                canvas.setString(0, 0, state[0].toString())
            }
        }
        val state = intArrayOf(0)

        terminal.draw { it.renderStatefulWidget(counter, it.area, state) }
        val completed = terminal.draw { it.renderStatefulWidget(counter, it.area, state) }

        assertEquals(2, state[0])
        assertEquals("2", completed.get(0, 0).symbol)
    }

    @Test
    fun `a widget is clipped to its area`() {
        val completed = terminal.draw { it.renderWidget(Label("overflow"), Rect(2, 0, 3, 1)) }

        assertEquals("  ove \n      ", text(completed))
    }

    @Test
    fun `the frame canvas exposes the same clipped region`() {
        val completed = terminal.draw { frame ->
            val canvas = frame.canvas(Rect(4, 1, 10, 10))
            assertEquals(Rect(0, 0, 2, 1), canvas.area)
            canvas.setString(0, 0, "ok")
        }

        assertEquals("ok", completed.get(4, 1).symbol + completed.get(5, 1).symbol)
    }
}
