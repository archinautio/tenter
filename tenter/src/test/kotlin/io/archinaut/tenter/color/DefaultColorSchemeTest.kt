// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.terminal.renderBuffer
import io.archinaut.tenter.widget.RowWriter
import io.archinaut.tenter.widget.CheckState
import io.archinaut.tenter.widget.SelectableRow

internal class DefaultColorSchemeTest {

    @Test
    fun `default color scheme resolves every standard role through widget rendering`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.ANSI16)
        val terminal = Terminal(ansiLevel = AnsiLevel.ANSI16, terminalInterface = recorder)
        val buffer = Buffer(40, 4)
        SelectableRow(label = "default", checkState = CheckState.CHECKED, selected = true).render(Canvas.of(buffer))
        for ((index, role) in StandardRole.entries.withIndex()) {
            buffer.set(index, 2, Cell("X", Style(fg = role, bg = role)))
        }

        io.archinaut.tenter.terminal.Terminal(terminal).openScreen(ColorScheme.Default).also { recorder.clearOutput() }.renderBuffer(buffer)

        for (role in StandardRole.entries) {
            assertTrue(ColorScheme.Default.foreground(role) is TerminalColor.Ansi16)
        }
        assertEquals(TerminalColor.Ansi16(30), ColorScheme.Default.defaultBackground)
        assertTrue(recorder.output().contains("default"))
    }
}
