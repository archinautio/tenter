// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

import com.github.ajalt.mordant.rendering.AnsiLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MapColorSchemeTest {

    /** A role the color scheme was never given. */
    private object Unknown : ColorRole

    @Test
    fun `a map color scheme requires every toolkit standard role before rendering`() {
        val exception = assertThrows<IllegalArgumentException> {
            MapColorScheme(
                name = "test",
                level = AnsiLevel.TRUECOLOR,
                defaultBackground = TerminalColor.TrueColor(0, 0, 0),
                colors = emptyMap(),
            )
        }

        assertEquals(true, exception.message?.contains("standard roles"))
    }

    @Test
    fun `a map color scheme validates its declared color tier`() {
        assertThrows<IllegalArgumentException> {
            MapColorScheme(
                name = "test",
                level = AnsiLevel.TRUECOLOR,
                defaultBackground = TerminalColor.Ansi16(30),
                colors = standardColors(TerminalColor.TrueColor(255, 255, 255)),
            )
        }
    }

    @Test
    fun `a map color scheme rejects NONE, which names a terminal rather than an authoring tier`() {
        val exception = assertThrows<IllegalArgumentException> {
            MapColorScheme(
                name = "test",
                level = AnsiLevel.NONE,
                defaultBackground = TerminalColor.Ansi16(30),
                colors = standardColors(TerminalColor.Ansi16(97)),
            )
        }

        assertEquals(true, exception.message?.contains("AnsiLevel.NONE"))
    }

    @Test
    fun `a map color scheme copies source colors and resolves semantic roles`() {
        val accent = TerminalColor.TrueColor(255, 0, 0)
        val source = standardColors(TerminalColor.TrueColor(255, 255, 255)).toMutableMap()
        source[StandardRole.ACCENT] = accent
        val colorScheme = MapColorScheme(
            name = "test",
            level = AnsiLevel.TRUECOLOR,
            defaultBackground = TerminalColor.TrueColor(0, 0, 0),
            colors = source,
        )

        source[StandardRole.ACCENT] = TerminalColor.TrueColor(0, 255, 0)

        assertEquals(accent, colorScheme.foreground(StandardRole.ACCENT))
        assertThrows<IllegalStateException> {
            colorScheme.foreground(Unknown)
        }
    }

    private fun standardColors(color: TerminalColor): Map<ColorRole, TerminalColor> =
        StandardRole.entries.associateWith { color }
}
