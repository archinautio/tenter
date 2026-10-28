// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ColorSpaceTest {

    @Test
    fun `each color space parses its own syntax into a color of that space`() {
        assertEquals(TerminalColor.TrueColor(255, 128, 0), ColorSpace.TRUECOLOR.parse("#ff8000"))
        assertEquals(TerminalColor.Ansi256(208), ColorSpace.ANSI256.parse(" 208 "))
        assertEquals(TerminalColor.Ansi16(91), ColorSpace.ANSI16.parse("91"))
    }

    @Test
    fun `a color reports the space it was authored in`() {
        assertEquals(ColorSpace.TRUECOLOR, TerminalColor.TrueColor(0, 0, 0).space)
        assertEquals(ColorSpace.ANSI256, TerminalColor.Ansi256(16).space)
        assertEquals(ColorSpace.ANSI16, TerminalColor.Ansi16(30).space)
    }

    @Test
    fun `a malformed or out-of-range value is rejected`() {
        assertThrows<IllegalArgumentException> { ColorSpace.TRUECOLOR.parse("ff8000") }
        assertThrows<IllegalArgumentException> { ColorSpace.ANSI256.parse("15") }
        assertThrows<IllegalArgumentException> { ColorSpace.ANSI16.parse("38") }
        assertThrows<IllegalArgumentException> { ColorSpace.ANSI16.parse("red") }
    }
}
