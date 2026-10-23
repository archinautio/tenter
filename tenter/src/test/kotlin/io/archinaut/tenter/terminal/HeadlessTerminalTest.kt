// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.mordant.rendering.AnsiLevel
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.Widget
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

internal class HeadlessTerminalTest {

    private val terminal = Terminal.headless(width = 6, height = 2)

    @Test
    fun `a headless terminal has its own size, no color, and no input`() {
        assertEquals(Rect(0, 0, 6, 2), terminal.area)
        assertEquals(AnsiLevel.NONE, terminal.ansiLevel)
        assertEquals(emptyList<TerminalEvent>(), runBlocking { terminal.events().toList() })
    }

    @Test
    fun `a headless terminal draws outside a scope`() {
        val frame = terminal.draw { it.renderWidget(Text("hi"), Rect(1, 1, 5, 1)) }

        assertEquals(listOf("      ", " hi   "), frame.lines())
    }

    @Test
    fun `lastFrame is what the latest draw returned and survives clear`() {
        assertNull(terminal.lastFrame)

        val drawn = terminal.draw { it.renderWidget(Text("one"), it.area) }
        terminal.clear()

        assertSame(drawn, terminal.lastFrame)
    }

    @Test
    fun `a failed draw keeps the previous lastFrame`() {
        val drawn = terminal.draw { it.renderWidget(Text("one"), it.area) }

        assertThrows<IllegalStateException> { terminal.draw { error("render failed") } }

        assertSame(drawn, terminal.lastFrame)
        assertEquals("one   ", drawn.lines().first())
    }

    @Test
    fun `lines show a wide character once`() {
        val frame = terminal.draw { it.renderWidget(Text("日本"), it.area) }

        assertEquals("日本  ", frame.lines().first())
    }
}
