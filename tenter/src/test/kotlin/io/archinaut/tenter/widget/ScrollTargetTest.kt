// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ScrollTargetTest {

    @Test
    fun `keep clamps its offset to the content`() {
        assertEquals(0, ScrollTarget.Keep(-3).offset(viewportSize = 5, maxOffset = 10))
        assertEquals(10, ScrollTarget.Keep(30).offset(viewportSize = 5, maxOffset = 10))
    }

    @Test
    fun `follow leaves an already visible range where it is`() {
        assertEquals(4, ScrollTarget.Follow(from = 4, start = 6, end = 7).offset(viewportSize = 5, maxOffset = 10))
    }

    @Test
    fun `follow makes the smallest move to a range above or below the window`() {
        assertEquals(2, ScrollTarget.Follow(from = 4, start = 2, end = 3).offset(viewportSize = 5, maxOffset = 10))
        assertEquals(8, ScrollTarget.Follow(from = 4, start = 12, end = 13).offset(viewportSize = 5, maxOffset = 10))
    }

    @Test
    fun `follow aligns a range longer than the viewport by the edge it is reached from`() {
        assertEquals(3, ScrollTarget.Follow(from = 8, start = 3, end = 12).offset(viewportSize = 5, maxOffset = 10))
        assertEquals(7, ScrollTarget.Follow(from = 0, start = 3, end = 12).offset(viewportSize = 5, maxOffset = 10))
    }

    @Test
    fun `center puts the range in the middle of the window`() {
        assertEquals(8, ScrollTarget.Center(start = 10, end = 11).offset(viewportSize = 5, maxOffset = 20))
    }

    @Test
    fun `nothing moves content that fits`() {
        assertEquals(0, ScrollTarget.Follow(from = 3, start = 4, end = 5).offset(viewportSize = 5, maxOffset = 0))
        assertEquals(0, ScrollTarget.Center(start = 4, end = 5).offset(viewportSize = 5, maxOffset = 0))
    }
}
