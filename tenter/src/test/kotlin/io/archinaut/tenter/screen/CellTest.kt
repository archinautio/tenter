// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.color.StandardRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class CellTest {

    private val painted = Cell("a", fg = StandardRole.DANGER, bg = StandardRole.SUCCESS, bold = true)

    @Test
    fun `painting keeps every channel the style leaves unset`() {
        assertEquals(
            Cell("b", fg = StandardRole.INFO, bg = StandardRole.SUCCESS, bold = true),
            painted.painted("b", Style(fg = StandardRole.INFO)),
        )
    }

    @Test
    fun `painting replaces every channel the style sets, back to the default too`() {
        assertEquals(
            Cell("b", bg = StandardRole.SUCCESS),
            painted.painted("b", Style(fg = StandardRole.DEFAULT, bold = false)),
        )
    }

    @Test
    fun `painting with the default style changes only the symbol`() {
        assertEquals(painted.copy(symbol = "b"), painted.painted("b", Style.DEFAULT))
        assertSame(painted, painted.painted("a", Style.DEFAULT))
    }

    @Test
    fun `cells look alike when only their symbols differ`() {
        assertTrue(painted.looksLike(painted.copy(symbol = "z")))
        assertFalse(painted.looksLike(painted.copy(bold = false)))
    }
}
