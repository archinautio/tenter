// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.palette.ChromeRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StyleTest {

    @Test
    fun `patching keeps every channel the patch leaves unset`() {
        val painted = Style(fg = ChromeRole.INFO, bg = ChromeRole.SUCCESS, strikethrough = true)

        assertEquals(painted, painted.patch(Style.DEFAULT))
        assertEquals(
            Style(fg = ChromeRole.DANGER, bg = ChromeRole.SUCCESS, strikethrough = true),
            painted.patch(Style(fg = ChromeRole.DANGER)),
        )
    }

    @Test
    fun `patching replaces every channel the patch sets, including back to the default`() {
        val painted = Style(fg = ChromeRole.INFO, bg = ChromeRole.SUCCESS, strikethrough = true)

        assertEquals(
            Style(fg = ChromeRole.INFO, bg = ChromeRole.DEFAULT, strikethrough = false),
            painted.patch(Style(bg = ChromeRole.DEFAULT, strikethrough = false)),
        )
    }
}
