// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.palette.ChromeRole

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class BufferTest {

    @Test
    fun `new buffer has all cells blank with no style channel set`() {
        val buffer = Buffer(3, 2)

        val defaultCell = Cell(" ", Style.DEFAULT)
        for (x in 0 until 3) {
            for (y in 0 until 2) {
                assertEquals(defaultCell, buffer.get(x, y))
            }
        }
    }

    @Test
    fun `set and get cell`() {
        val buffer = Buffer(5, 5)
        val cell = Cell("A", Style(ChromeRole.DANGER, ChromeRole.INFO))

        buffer.set(2, 3, cell)

        assertEquals(cell, buffer.get(2, 3))
    }

    @Test
    fun `set out of bounds is ignored`() {
        val buffer = Buffer(3, 3)

        buffer.set(-1, 0, Cell("X"))
        buffer.set(0, -1, Cell("X"))
        buffer.set(3, 0, Cell("X"))
        buffer.set(0, 3, Cell("X"))

        assertEquals(Cell(), buffer.get(0, 0))
    }

    @Test
    fun `get out of bounds throws`() {
        val buffer = Buffer(3, 3)

        assertThrows<IndexOutOfBoundsException> { buffer.get(-1, 0) }
        assertThrows<IndexOutOfBoundsException> { buffer.get(3, 0) }
        assertThrows<IndexOutOfBoundsException> { buffer.get(0, -1) }
        assertThrows<IndexOutOfBoundsException> { buffer.get(0, 3) }
    }

    @Test
    fun `width and height are accessible`() {
        val buffer = Buffer(10, 20)

        assertEquals(10, buffer.width)
        assertEquals(20, buffer.height)
    }

    @Test
    fun `negative dimensions are rejected while zero dimensions are valid`() {
        assertThrows<IllegalArgumentException> { Buffer(-1, 0) }
        assertThrows<IllegalArgumentException> { Buffer(0, -1) }

        val empty = Buffer(0, 0)
        assertEquals(0, empty.width)
        assertEquals(0, empty.height)
    }

    @Test
    fun `set rejects a multi-grapheme or control cell`() {
        val buffer = Buffer(2, 1)

        assertThrows<IllegalArgumentException> { buffer.set(0, 0, Cell("AB")) }
        assertThrows<IllegalArgumentException> { buffer.set(0, 0, Cell("\u001B")) }
    }

    @Test
    fun `an observed continuation cannot create an orphan when written back`() {
        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "中")

        buffer.set(1, 0, buffer.get(1, 0))

        assertEquals(Cell(), buffer.get(0, 0))
        assertEquals(Cell(), buffer.get(1, 0))
    }
}
