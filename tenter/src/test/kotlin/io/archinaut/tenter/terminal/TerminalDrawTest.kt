// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.color.TerminalColor
import io.archinaut.tenter.color.ColorScheme
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

private object PlainColorScheme : ColorScheme {
    override val defaultBackground: TerminalColor = TerminalColor.TrueColor(0, 0, 0)

    override fun foreground(role: ColorRole): TerminalColor = TerminalColor.TrueColor(255, 255, 255)
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

    private val device = ResizableRecorder(6, 2)
    private val terminal = Terminal(device.backend)
    private val screen = terminal.openScreen(PlainColorScheme)

    @Test
    fun `the area starts at the backend's size`() {
        assertEquals(Rect(0, 0, 6, 2), screen.area)
    }

    @Test
    fun `draw renders widgets into the frame and returns the completed cells`() {
        val completed = screen.draw { frame ->
            assertEquals(Rect(0, 0, 6, 2), frame.area)
            frame.renderWidget(Label("hi"), Rect(1, 1, 5, 1))
        }

        assertEquals("      \n hi   ", text(completed))
        assertTrue(device.recorder.output().contains("hi"))
    }

    @Test
    fun `an identical second frame sends nothing`() {
        screen.draw { it.renderWidget(Label("same"), it.area) }
        device.recorder.clearOutput()

        screen.draw { it.renderWidget(Label("same"), it.area) }

        assertEquals("", device.recorder.output())
    }

    @Test
    fun `each frame starts blank, so dropping a widget erases it`() {
        screen.draw { it.renderWidget(Label("gone"), it.area) }
        device.recorder.clearOutput()

        val completed = screen.draw { }

        assertEquals("      \n      ", text(completed))
        assertTrue(device.recorder.output().contains("    "), "expected the old cells to be blanked")
    }

    @Test
    fun `only the changed cells are sent`() {
        screen.draw { it.renderWidget(Label("hello"), it.area) }
        device.recorder.clearOutput()

        screen.draw { it.renderWidget(Label("hallo"), it.area) }

        val out = device.recorder.output()
        assertTrue(out.contains("a"), "expected the changed cell")
        assertTrue(!out.contains("llo"), "expected unchanged cells to be skipped: $out")
    }

    @Test
    fun `a failed draw sends nothing and keeps the previous frame as the diff base`() {
        screen.draw { it.renderWidget(Label("kept"), it.area) }
        device.recorder.clearOutput()
        val failure = IllegalStateException("render failed")

        val thrown = assertThrows<IllegalStateException> {
            screen.draw { frame ->
                frame.renderWidget(Label("half"), frame.area)
                throw failure
            }
        }

        assertSame(failure, thrown)
        assertEquals("", device.recorder.output())
        screen.draw { it.renderWidget(Label("kept"), it.area) }
        assertEquals("", device.recorder.output(), "the failed frame must not become the diff base")
    }

    @Test
    fun `draw follows a backend resize with a full repaint`() {
        screen.draw { it.renderWidget(Label("a"), it.area) }
        device.size = Size(4, 3)
        device.backend.updateSize()
        device.recorder.clearOutput()

        val completed = screen.draw { it.renderWidget(Label("a"), it.area) }

        assertEquals(Rect(0, 0, 4, 3), completed.area)
        assertTrue(device.recorder.output().contains("a"), "a resized frame must repaint fully")
    }

    @Test
    fun `an explicit resize holds until the backend size changes`() {
        terminal.resize(3, 1)

        assertEquals(Rect(0, 0, 3, 1), screen.draw { }.area)
        assertEquals(Rect(0, 0, 3, 1), screen.draw { }.area)

        device.size = Size(5, 2)
        device.backend.updateSize()

        assertEquals(Rect(0, 0, 5, 2), screen.draw { }.area)
    }

    @Test
    fun `a new screen repaints its first frame fully`() {
        screen.draw { it.renderWidget(Label("x"), it.area) }
        terminal.closeScreen(screen, failure = null)
        val next = terminal.openScreen(PlainColorScheme)
        device.recorder.clearOutput()

        next.draw { it.renderWidget(Label("x"), it.area) }

        assertTrue(device.recorder.output().contains("x"))
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

        screen.draw { it.renderStatefulWidget(counter, it.area, state) }
        val completed = screen.draw { it.renderStatefulWidget(counter, it.area, state) }

        assertEquals(2, state[0])
        assertEquals("2", completed.get(0, 0).symbol)
    }

    @Test
    fun `a widget is clipped to its area`() {
        val completed = screen.draw { it.renderWidget(Label("overflow"), Rect(2, 0, 3, 1)) }

        assertEquals("  ove \n      ", text(completed))
    }

    @Test
    fun `a widget rendered into an area receives that clipped region`() {
        var area: Rect? = null
        val widget = object : Widget {
            override fun render(canvas: Canvas) {
                area = canvas.area
                canvas.setString(0, 0, "ok")
            }
        }

        val completed = screen.draw { frame -> frame.renderWidget(widget, Rect(4, 1, 10, 10)) }

        assertEquals(Rect(0, 0, 2, 1), area)

        assertEquals("ok", completed.get(4, 1).symbol + completed.get(5, 1).symbol)
    }
}
