// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RectTest {

    private val bounds = Rect(0, 0, 10, 4)

    @Test
    fun `clipTo keeps the overlapping part`() {
        assertEquals(Rect(0, 1, 3, 3), Rect(-2, 1, 5, 6).clipTo(bounds))
    }

    @Test
    fun `clipTo leaves a contained rectangle unchanged`() {
        assertEquals(Rect(2, 1, 3, 2), Rect(2, 1, 3, 2).clipTo(bounds))
    }

    @Test
    fun `clipTo of a disjoint rectangle is empty on the nearest edge of the bounds`() {
        assertEquals(Rect(10, 0, 0, 1), Rect(15, 0, 3, 1).clipTo(bounds))
        assertEquals(Rect(0, 0, 0, 1), Rect(-5, 0, 2, 1).clipTo(bounds))
    }

    @Test
    fun `offset moves the origin and keeps the size`() {
        assertEquals(Rect(3, -1, 2, 2), Rect(1, 1, 2, 2).offset(2, -2))
    }

    @Test
    fun `isEmpty when either dimension is zero`() {
        assertTrue(Rect(3, 3, 0, 5).isEmpty)
        assertTrue(Rect(3, 3, 5, 0).isEmpty)
        assertFalse(Rect(3, 3, 1, 1).isEmpty)
    }
}
