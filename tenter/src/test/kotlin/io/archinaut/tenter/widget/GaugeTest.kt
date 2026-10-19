// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.palette.ChromeRole

internal class GaugeTest {

    private fun paint(gauge: Gauge, width: Int = 28): Buffer = render(gauge, width, 5)

    @Test
    fun `clamps out-of-range values before narrowing the scaled fill`() {
        val high = paint(Gauge(value = Int.MAX_VALUE, maxValue = 1, barWidth = 2))
        val low = paint(Gauge(value = Int.MIN_VALUE, maxValue = 1, barWidth = 2))

        assertEquals("█", high.get(1, 0).symbol)
        assertEquals("█", high.get(2, 0).symbol)
        assertEquals("░", low.get(1, 0).symbol)
        assertEquals("░", low.get(2, 0).symbol)
    }

    @Test
    fun `draws empty bar with suffix`() {
        val buffer = paint(Gauge(value = 0, maxValue = 30, barWidth = 20))

        assertTrue(buffer.line(0).contains("[" + "░".repeat(20) + "]30"))
        assertEquals("0", buffer.get(1, 1).symbol)
    }

    @Test
    fun `draws proportional fill`() {
        val buffer = paint(Gauge(value = 15, maxValue = 30, barWidth = 20))

        assertTrue(buffer.line(0).contains("█".repeat(10) + "░".repeat(10)))
    }

    @Test
    fun `right-aligns value under last filled cell`() {
        val buffer = paint(Gauge(value = 15, maxValue = 30, barWidth = 20))

        assertEquals("1", buffer.get(9, 1).symbol)
        assertEquals("5", buffer.get(10, 1).symbol)
    }

    @Test
    fun `occupies two rows`() {
        val gauge = Gauge(value = 0, maxValue = 30, barWidth = 20)
        var used = 0
        var rowAfter = 0
        MeasurableWidget { cursor ->
            used = cursor.draw(gauge)
            rowAfter = cursor.row
        }.measure(28)

        assertEquals(2, gauge.measure(28).height)
        assertEquals(2, used)
        assertEquals(2, rowAfter)
    }

    @Test
    fun `colors danger at seventy percent of max`() {
        val buffer = paint(Gauge(value = 21, maxValue = 30, barWidth = 20))

        assertEquals(ChromeRole.DANGER, buffer.get(0, 0).style.fg)
        // value "21" is 2 chars; filled = 21*20/30 = 14, anchorCol = 14
        // "21" written at (14 - 2 + 1, 1) = (13, 1)
        assertEquals(ChromeRole.DANGER, buffer.get(13, 1).style.fg)
    }

    @Test
    fun `colors warning at thirty percent of max`() {
        val buffer = paint(Gauge(value = 9, maxValue = 30, barWidth = 20))

        assertEquals(ChromeRole.WARNING, buffer.get(0, 0).style.fg)
    }

    @Test
    fun `colors info (cool) below thirty percent`() {
        val buffer = paint(Gauge(value = 8, maxValue = 30, barWidth = 20))

        assertEquals(ChromeRole.INFO, buffer.get(0, 0).style.fg)
    }

    @Test
    fun `renders custom suffix after closing bracket`() {
        val buffer = paint(Gauge(value = 10, maxValue = 20, barWidth = 10, suffix = "DTS 10(20)"))

        val row0 = buffer.line(0)
        assertTrue(row0.contains("]DTS 10(20)"))
        assertTrue(row0.contains("█".repeat(5) + "░".repeat(5)))
    }

    @Test
    fun `renders empty bar when max is zero`() {
        val buffer = paint(Gauge(value = 0, maxValue = 0, barWidth = 10, suffix = "STS 0"))

        assertTrue(buffer.line(0).contains("░".repeat(10)))
        assertEquals(ChromeRole.DANGER, buffer.get(0, 0).style.fg)
    }

    @Test
    fun `rejects negative sizing parameters`() {
        assertThrows(IllegalArgumentException::class.java) { Gauge(value = 0, maxValue = 20, barWidth = -1) }
        assertThrows(IllegalArgumentException::class.java) { Gauge(value = 0, maxValue = -1, barWidth = 10) }
    }

    @Test
    fun `accepts an explicit color policy`() {
        val buffer = paint(Gauge(value = 20, maxValue = 20, barWidth = 10, colorFor = { ChromeRole.SUCCESS }))

        assertEquals(ChromeRole.SUCCESS, buffer.get(0, 0).style.fg)
    }

    @Test
    fun `uses wide arithmetic when scaling a large value`() {
        val buffer = paint(Gauge(value = Int.MAX_VALUE, maxValue = Int.MAX_VALUE, barWidth = 100), width = 110)

        assertEquals(ChromeRole.DANGER, buffer.get(0, 0).style.fg)
        assertEquals("█", buffer.get(99, 0).symbol)
    }
}
