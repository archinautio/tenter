// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Size
import io.archinaut.tenter.screen.Style

internal class GaugeTest {

    @Test
    fun `fills the bar in proportion to the ratio across the whole width`() {
        val buffer = render(Gauge(0.5), 10, 1)

        assertEquals("█████░░░░░", buffer.line(0))
        assertEquals(Size(10, 1), Gauge(0.5).measure(10))
    }

    @Test
    fun `a label precedes the bar, which takes the rest of the width`() {
        val buffer = render(Gauge(1.0, label = "HP"), 8, 1)

        assertEquals("HP █████", buffer.line(0))
    }

    @Test
    fun `empty and full gauges fill nothing and everything`() {
        assertEquals("░░░░", render(Gauge(0.0), 4, 1).line(0))
        assertEquals("████", render(Gauge(1.0), 4, 1).line(0))
    }

    @Test
    fun `the caller chooses the filled and unfilled styles`() {
        val buffer = render(Gauge(0.5, filledStyle = Style(ChromeRole.DANGER), unfilledStyle = Style(ChromeRole.INFO)), 4, 1)

        assertEquals(ChromeRole.DANGER, buffer.get(0, 0).style.fg)
        assertEquals(ChromeRole.INFO, buffer.get(3, 0).style.fg)
    }

    @Test
    fun `a ratio outside zero to one is rejected`() {
        assertThrows<IllegalArgumentException> { Gauge(-0.1) }
        assertThrows<IllegalArgumentException> { Gauge(1.1) }
        assertThrows<IllegalArgumentException> { Gauge(Double.NaN) }
    }
}
