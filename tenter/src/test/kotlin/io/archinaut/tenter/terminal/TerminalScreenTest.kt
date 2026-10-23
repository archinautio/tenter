// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.mordant.input.InputEvent
import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Size
import com.github.ajalt.mordant.terminal.PrintRequest
import com.github.ajalt.mordant.terminal.TerminalInterface
import com.github.ajalt.mordant.terminal.TerminalInfo
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.archinaut.tenter.palette.ColorRole
import io.archinaut.tenter.palette.PaletteColor
import io.archinaut.tenter.palette.RolePalette
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.widget.Widget
import kotlin.time.TimeMark
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal

private object ScreenTestPalette : RolePalette {
    override val defaultBackground: PaletteColor = PaletteColor.TrueColor(0, 0, 0)

    override fun foreground(role: ColorRole): PaletteColor = PaletteColor.TrueColor(255, 255, 255)
}

private object OtherScreenTestPalette : RolePalette {
    override val defaultBackground: PaletteColor = PaletteColor.TrueColor(0, 0, 255)

    override fun foreground(role: ColorRole): PaletteColor = PaletteColor.TrueColor(255, 255, 0)
}

private class ScreenLabel(private val text: String) : Widget {
    override fun render(canvas: Canvas) {
        canvas.setString(0, 0, text)
    }
}

internal class TerminalScreenTest {

    @Test
    fun `scope returns the block value and enters and exits once`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val terminal = Terminal(MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = recorder))

        val result = terminal.withScreen(ScreenTestPalette) { 42 }

        assertEquals(42, result)
        assertEquals(1, recorder.output().countOccurrences("\u001B[?1049h"))
        assertEquals(1, recorder.output().countOccurrences("\u001B[?1049l"))
    }

    @Test
    fun `a terminal enters a new scope after the previous one returned`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val terminal = Terminal(MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = recorder))

        terminal.withScreen(ScreenTestPalette) { }
        val second = terminal.withScreen(ScreenTestPalette) { it }

        assertSame(terminal, second)
        assertEquals(2, recorder.output().countOccurrences("\u001B[?1049h"))
        assertEquals(2, recorder.output().countOccurrences("\u001B[?1049l"))
    }

    @Test
    fun `a nested scope throws and leaves the outer scope drawing`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val terminal = Terminal(MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = recorder))

        terminal.withScreen(ScreenTestPalette) {
            assertThrows<IllegalStateException> { terminal.withScreen(ScreenTestPalette) { } }
            terminal.draw { frame -> frame.renderWidget(ScreenLabel("still drawing"), frame.area) }
        }

        assertEquals(1, recorder.output().countOccurrences("\u001B[?1049h"))
    }

    @Test
    fun `drawing outside a scope throws`() {
        val terminal = Terminal(MordantTerminal(terminalInterface = TerminalRecorder()))

        assertThrows<IllegalStateException> { terminal.draw { } }
        terminal.withScreen(ScreenTestPalette) { }
        assertThrows<IllegalStateException> { terminal.draw { } }
    }

    @Test
    fun `a scope draws with its own palette`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val terminal = Terminal(MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = recorder))

        terminal.withScreen(ScreenTestPalette) { }
        recorder.clearOutput()
        terminal.withScreen(OtherScreenTestPalette) { it.draw { frame -> frame.renderWidget(ScreenLabel("x"), frame.area) } }

        assertTrue(recorder.output().contains("48;2;0;0;255"), "expected the second palette's background: ${recorder.output()}")
    }

    @Test
    fun `scope restores the terminal after the block fails`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val terminal = Terminal(MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = recorder))
        val failure = IllegalStateException("block failed")

        val thrown = assertThrows<IllegalStateException> {
            terminal.withScreen(ScreenTestPalette) { throw failure }
        }

        assertSame(failure, thrown)
        assertEquals(1, recorder.output().countOccurrences("\u001B[?1049h"))
        assertEquals(1, recorder.output().countOccurrences("\u001B[?1049l"))
    }

    @Test
    fun `noninteractive output receives no lifecycle escapes`() {
        val recorder = TerminalRecorder(
            ansiLevel = AnsiLevel.NONE,
            outputInteractive = false,
            inputInteractive = false,
        )
        val terminal = Terminal(MordantTerminal(ansiLevel = AnsiLevel.NONE, terminalInterface = recorder))

        terminal.withScreen(ScreenTestPalette) { Unit }

        assertTrue(recorder.output().none { it == '\u001B' }, "unexpected escape output: ${recorder.output()}")
    }

    @Test
    fun `entry failure still attempts cleanup and preserves the entry failure`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val terminalInterface = FailingTerminalInterface(recorder, failOnCall = 2)
        val terminal = Terminal(MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = terminalInterface))

        val thrown = assertThrows<IllegalStateException> {
            terminal.withScreen(ScreenTestPalette) { throw AssertionError("unreachable") }
        }

        assertEquals("output failed", thrown.message)
        assertEquals(4, terminalInterface.printCalls)
        assertEquals(1, recorder.output().countOccurrences("\u001B[?1049l"))
    }

    @Test
    fun `cleanup failure is suppressed on a block failure`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val terminalInterface = FailingTerminalInterface(recorder, failFromCall = 4)
        val terminal = Terminal(MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = terminalInterface))
        val failure = IllegalStateException("block failed")

        val thrown = assertThrows<IllegalStateException> {
            terminal.withScreen(ScreenTestPalette) { throw failure }
        }

        assertSame(failure, thrown)
        assertEquals(1, thrown.suppressed.size)
        assertEquals("output failed", thrown.suppressed.single().message)
        assertEquals(1, thrown.suppressed.single().suppressed.size)
    }

    private class FailingTerminalInterface(
        private val delegate: TerminalRecorder,
        private val failOnCall: Int? = null,
        private val failFromCall: Int? = null,
    ) : TerminalInterface {
        var printCalls: Int = 0
            private set

        public override fun info(
            ansiLevel: AnsiLevel?,
            hyperlinks: Boolean?,
            outputInteractive: Boolean?,
            inputInteractive: Boolean?,
        ): TerminalInfo = delegate.info(ansiLevel, hyperlinks, outputInteractive, inputInteractive)

        public override fun completePrintRequest(request: PrintRequest) {
            printCalls++
            if (printCalls == failOnCall || (failFromCall != null && printCalls >= failFromCall)) {
                throw IllegalStateException("output failed")
            }
            delegate.completePrintRequest(request)
        }

        public override fun readLineOrNull(hideInput: Boolean): String? = delegate.readLineOrNull(hideInput)

        public override fun getTerminalSize(): Size = delegate.getTerminalSize()

        public override fun readInputEvent(timeout: TimeMark, mouseTracking: MouseTracking): InputEvent? =
            delegate.readInputEvent(timeout, mouseTracking)

        public override fun enterRawMode(mouseTracking: MouseTracking): AutoCloseable = delegate.enterRawMode(mouseTracking)

        public override fun shouldAutoUpdateSize(): Boolean = delegate.shouldAutoUpdateSize()
    }

    private fun String.countOccurrences(substring: String): Int {
        var count = 0
        var start = 0
        while (true) {
            val index = indexOf(substring, start)
            if (index < 0) return count
            count++
            start = index + substring.length
        }
    }
}
