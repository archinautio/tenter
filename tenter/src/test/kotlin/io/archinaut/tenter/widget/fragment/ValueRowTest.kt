// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.fragment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.widget.TextCursor

internal class ValueRowTest {

    @Test
    fun `places a right value using display width after a wide left value`() {
        val buffer = Buffer(10, 1)
        val content = TextCursor(Canvas.of(buffer))

        ValueRow.draw(content, left = "中", right = "R", subLines = emptyList(), color = ChromeRole.TEXT_PRIMARY)

        assertEquals("中", buffer.get(0, 0).symbol)
        assertEquals("R", buffer.get(9, 0).symbol)
        assertEquals(1, content.row)
    }
}
