// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.widget.Widget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class Text(private val text: String) : Widget {
    override fun render(canvas: Canvas) {
        canvas.setString(0, 0, text)
    }
}

private fun Screen.drawText(text: String): CompletedFrame = draw { it.renderWidget(Text(text), it.area) }

private fun TerminalEvent.key(): String? = ((this as? TerminalEvent.Input)?.event as? KeyboardEvent)?.key

/** Collects [this] until the key [quit], drawing each key it reads, as an application loop would. */
private fun Flow<TerminalEvent>.echoUntil(screen: Screen, quit: String): List<TerminalEvent> = runBlocking {
    takeWhile { it.key() != quit }
        .onEach { event -> event.key()?.let { screen.drawText(it) } }
        .toList()
}

internal class HeadlessTerminalTest {

    @Test
    fun `a headless terminal has its own size and no color`() {
        val terminal = Terminal.headless(width = 6, height = 2)

        assertNull(terminal.colorSpace)
        assertEquals(Rect(0, 0, 6, 2), terminal.withScreen { it.area })
    }

    @Test
    fun `the frame is blank before the first draw and keeps the last one after the scope`() {
        val terminal = Terminal.headless(width = 3, height = 1)
        assertEquals(listOf("   "), terminal.frame.lines())

        val drawn = terminal.withScreen { it.drawText("hi") }

        assertSame(drawn, terminal.frame)
        assertEquals(listOf("hi "), terminal.frame.lines())
    }

    @Test
    fun `entering a screen blanks the frame`() {
        val terminal = Terminal.headless(width = 3, height = 1)
        terminal.withScreen { it.drawText("hi") }

        terminal.withScreen { assertEquals(listOf("   "), terminal.frame.lines()) }
    }

    @Test
    fun `a drawn frame stays as it was after later draws`() {
        val terminal = Terminal.headless(width = 6, height = 2)

        terminal.withScreen { screen ->
            val first = screen.draw { it.renderWidget(Text("hi"), Rect(1, 1, 5, 1)) }
            screen.drawText("later")

            assertEquals(listOf("      ", " hi   "), first.lines())
        }
    }

    @Test
    fun `a failed draw keeps the previous frame`() {
        val terminal = Terminal.headless(width = 6, height = 1)

        terminal.withScreen { screen ->
            val drawn = screen.drawText("one")
            assertThrows<IllegalStateException> { screen.draw { error("render failed") } }

            assertSame(drawn, terminal.frame)
        }
    }

    @Test
    fun `lines show a wide character once`() {
        val terminal = Terminal.headless(width = 6, height = 1)

        val frame = terminal.withScreen { it.drawText("日本") }

        assertEquals("日本  ", frame.lines().first())
    }

    @Test
    fun `events start with the size and end when the script returns`() {
        val terminal = Terminal.headless(width = 6, height = 2) { key("a") }

        val events = terminal.withScreen { screen -> runBlocking { screen.events().toList() } }

        assertEquals(listOf(TerminalEvent.Resized(Size(6, 2)), TerminalEvent.Input(KeyboardEvent("a"))), events)
    }

    @Test
    fun `a coroutine suspends inside the screen scope`() {
        val terminal = Terminal.headless(width = 3, height = 1) { key("a") }

        val events = runBlocking { terminal.withScreen { screen -> screen.events().toList() } }

        assertEquals(2, events.size)
    }

    @Test
    fun `the script sees what the application drew for its previous event`() {
        val seen = mutableListOf<String>()
        val terminal = Terminal.headless(width = 3, height = 1) {
            key("a")
            seen += frame.lines().single()
            key("b")
            seen += frame.lines().single()
        }

        terminal.withScreen { screen -> screen.events().echoUntil(screen, quit = "q") }

        assertEquals(listOf("a  ", "b  "), seen)
    }

    @Test
    fun `the script carries on in the next collection, which starts with the size again`() {
        val terminal = Terminal.headless(width = 3, height = 1) {
            key("a")
            key("q")
            key("b")
        }

        val (first, second) = terminal.withScreen { screen ->
            screen.events().echoUntil(screen, quit = "q") to screen.events().echoUntil(screen, quit = "q")
        }

        val resized = TerminalEvent.Resized(Size(3, 1))
        assertEquals(listOf(resized, TerminalEvent.Input(KeyboardEvent("a"))), first)
        assertEquals(listOf(resized, TerminalEvent.Input(KeyboardEvent("b"))), second)
    }

    @Test
    fun `the script stops at the event that ended the application's reading`() {
        var afterQuit = false
        val terminal = Terminal.headless(width = 3, height = 1) {
            key("q")
            afterQuit = true
        }

        terminal.withScreen { screen -> screen.events().echoUntil(screen, quit = "q") }

        assertEquals(false, afterQuit)
    }

    @Test
    fun `a failing script fails the collection`() {
        val terminal = Terminal.headless(width = 3, height = 1) {
            key("a")
            error("script failed")
        }

        val thrown = assertThrows<IllegalStateException> {
            terminal.withScreen { screen -> screen.events().echoUntil(screen, quit = "q") }
        }

        assertEquals("script failed", thrown.message)
    }

    @Test
    fun `resizing from the script changes the area and sends the new size`() {
        val terminal = Terminal.headless(width = 3, height = 1) { resize(5, 2) }

        val (events, area) = terminal.withScreen { screen -> runBlocking { screen.events().toList() } to screen.area }

        assertEquals(TerminalEvent.Resized(Size(5, 2)), events.last())
        assertEquals(Rect(0, 0, 5, 2), area)
    }
}
