// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.color.StandardRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StyleTest {

    @Test
    fun `patching keeps every channel the patch leaves unset`() {
        val painted = Style(fg = StandardRole.INFO, bg = StandardRole.SUCCESS, strikethrough = true)

        assertEquals(painted, painted.patch(Style.DEFAULT))
        assertEquals(
            Style(fg = StandardRole.DANGER, bg = StandardRole.SUCCESS, strikethrough = true),
            painted.patch(Style(fg = StandardRole.DANGER)),
        )
    }

    @Test
    fun `patching replaces every channel the patch sets, including back to the default`() {
        val painted = Style(fg = StandardRole.INFO, bg = StandardRole.SUCCESS, strikethrough = true)

        assertEquals(
            Style(fg = StandardRole.INFO, bg = StandardRole.DEFAULT, strikethrough = false),
            painted.patch(Style(bg = StandardRole.DEFAULT, strikethrough = false)),
        )
    }

    @Test
    fun `patching merges text attributes channel by channel`() {
        val painted = Style(bold = true, underline = true, inverse = true)

        assertEquals(
            Style(bold = false, dim = true, italic = true, underline = true, inverse = true),
            painted.patch(Style(bold = false, dim = true, italic = true)),
        )
    }
}
