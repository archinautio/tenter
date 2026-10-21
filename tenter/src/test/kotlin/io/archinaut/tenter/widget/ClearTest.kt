// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ClearTest {

    private val painted = Style(fg = ChromeRole.DANGER, bg = ChromeRole.SUCCESS, strikethrough = true)

    @Test
    fun `clear resets every cell of its area to an unpainted blank`() {
        val canvas = Canvas.offscreen(4, 2)
        canvas.setString(0, 0, "ABCD", painted)
        canvas.setString(0, 1, "EFGH", painted)

        Clear.render(canvas.region(Rect(1, 0, 2, 2)))

        assertEquals(Cell("A", painted), canvas.get(0, 0))
        assertEquals(Cell(" ", Style.DEFAULT), canvas.get(1, 0))
        assertEquals(Cell(" ", Style.DEFAULT), canvas.get(2, 1))
        assertEquals(Cell("H", painted), canvas.get(3, 1))
    }

    @Test
    fun `content drawn after clear does not inherit what was painted beneath`() {
        val canvas = Canvas.offscreen(1, 1)
        canvas.set(0, 0, Cell("X", painted))

        Clear.render(canvas)
        canvas.set(0, 0, Cell("─", Style(fg = ChromeRole.ACCENT)))

        assertEquals(Cell("─", Style(fg = ChromeRole.ACCENT)), canvas.get(0, 0))
    }

    @Test
    fun `a wide glyph straddling the area's edge is left whole, as any clipped write leaves it`() {
        val canvas = Canvas.offscreen(3, 1)
        canvas.setString(0, 0, "中x", painted)

        Clear.render(canvas.region(Rect(1, 0, 2, 1)))

        assertEquals(Cell("中", painted), canvas.get(0, 0))
        assertEquals(Cell(" ", Style.DEFAULT), canvas.get(2, 0))
    }
}
