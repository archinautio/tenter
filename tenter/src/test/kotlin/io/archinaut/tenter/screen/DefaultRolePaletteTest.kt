// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.palette.PaletteColor
import io.archinaut.tenter.palette.DefaultRolePalette

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.terminal.renderBuffer
import io.archinaut.tenter.widget.TextCursor
import io.archinaut.tenter.widget.CheckState
import io.archinaut.tenter.widget.SelectableRow

internal class DefaultRolePaletteTest {

    @Test
    fun `default palette resolves every chrome role through widget rendering`() {
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.ANSI16)
        val terminal = Terminal(ansiLevel = AnsiLevel.ANSI16, terminalInterface = recorder)
        val buffer = Buffer(40, 4)
        val content = TextCursor(Canvas.of(buffer))

        content.draw(SelectableRow(label = "default", checkState = CheckState.CHECKED, selected = true))
        for ((index, role) in ChromeRole.entries.withIndex()) {
            buffer.set(index, 2, Cell("X", Style(fg = role, bg = role)))
        }

        io.archinaut.tenter.terminal.Terminal(terminal, DefaultRolePalette).renderBuffer(buffer)

        for (role in ChromeRole.entries) {
            assertTrue(DefaultRolePalette.foreground(role) is PaletteColor.Ansi16)
        }
        assertEquals(PaletteColor.Ansi16(30), DefaultRolePalette.defaultBackground)
        assertTrue(recorder.output().contains("default"))
    }
}
