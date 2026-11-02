// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

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
                space = ColorSpace.TRUECOLOR,
                defaultBackground = TerminalColor.TrueColor(0, 0, 0),
                colors = emptyMap(),
            )
        }

        assertEquals(true, exception.message?.contains("standard roles"))
    }

    @Test
    fun `a map color scheme rejects a default background outside its color space`() {
        assertThrows<IllegalArgumentException> {
            MapColorScheme(
                name = "test",
                space = ColorSpace.TRUECOLOR,
                defaultBackground = TerminalColor.Ansi16(30),
                colors = standardColors(TerminalColor.TrueColor(255, 255, 255)),
            )
        }
    }

    @Test
    fun `a map color scheme rejects a role color outside its color space`() {
        val exception = assertThrows<IllegalArgumentException> {
            MapColorScheme(
                name = "test",
                space = ColorSpace.ANSI16,
                defaultBackground = TerminalColor.Ansi16(30),
                colors = standardColors(TerminalColor.Ansi16(97)) + (StandardRole.ACCENT to TerminalColor.Ansi256(208)),
            )
        }

        assertEquals(true, exception.message?.contains("ANSI16"))
    }

    @Test
    fun `a map color scheme copies source colors and resolves semantic roles`() {
        val accent = TerminalColor.TrueColor(255, 0, 0)
        val source = standardColors(TerminalColor.TrueColor(255, 255, 255)).toMutableMap()
        source[StandardRole.ACCENT] = accent
        val colorScheme = MapColorScheme(
            name = "test",
            space = ColorSpace.TRUECOLOR,
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
