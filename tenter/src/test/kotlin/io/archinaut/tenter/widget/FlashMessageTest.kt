// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.palette.ChromeRole

internal class FlashMessageTest {

    @Test
    fun `renders one ellipsized row in its style`() {
        val message = FlashMessage("saved all changes")

        val buffer = render(message, 10, 2)

        assertEquals(1, message.measure(10).height)
        assertEquals("saved all…", buffer.line(0))
        assertEquals(ChromeRole.INFO, buffer.get(0, 0).style.fg)
    }
}
