// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

import io.archinaut.tenter.screen.Constraint.Fill
import io.archinaut.tenter.screen.Constraint.Length
import io.archinaut.tenter.screen.Constraint.Max
import io.archinaut.tenter.screen.Constraint.Min
import io.archinaut.tenter.screen.Constraint.Percentage
import io.archinaut.tenter.screen.Constraint.Ratio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class LayoutTest {

    private val area = Rect(2, 3, 20, 10)

    private fun heights(layout: Layout, rect: Rect = area): List<Int> = layout.split(rect).map { it.height }

    @Test
    fun `vertical split stacks segments over the full width`() {
        val (header, body, footer) = Layout.vertical(Length(1), Fill(1), Length(2)).split(area)

        assertEquals(Rect(2, 3, 20, 1), header)
        assertEquals(Rect(2, 4, 20, 7), body)
        assertEquals(Rect(2, 11, 20, 2), footer)
    }

    @Test
    fun `horizontal split places segments side by side over the full height`() {
        val (left, right) = Layout.horizontal(Length(5), Fill(1)).split(area)

        assertEquals(Rect(2, 3, 5, 10), left)
        assertEquals(Rect(7, 3, 15, 10), right)
    }

    @Test
    fun `fill segments share leftover space by weight with remainders to the earliest`() {
        assertEquals(listOf(4, 6), heights(Layout.vertical(Fill(2), Fill(3))))
        assertEquals(listOf(4, 3, 3), heights(Layout.vertical(Fill(1), Fill(1), Fill(1))))
    }

    @Test
    fun `percentage and ratio take a rounded-down share of the available space`() {
        assertEquals(listOf(3, 3, 0), heights(Layout.vertical(Percentage(33), Ratio(1, 3), Length(0))))
    }

    @Test
    fun `min grows into leftover space only when there is no fill`() {
        assertEquals(listOf(2, 8), heights(Layout.vertical(Length(2), Min(3))))
        assertEquals(listOf(3, 7), heights(Layout.vertical(Min(3), Fill(1))))
    }

    @Test
    fun `max asks for its maximum and leftover space stays empty after the last segment`() {
        val rects = Layout.vertical(Max(3), Length(2)).split(area)

        assertEquals(listOf(3, 2), rects.map { it.height })
        assertEquals(6, rects.last().y)
    }

    @Test
    fun `a deficit shrinks the last segments first and min segments last`() {
        assertEquals(listOf(6, 4), heights(Layout.vertical(Length(6), Length(6))))
        assertEquals(listOf(6, 4), heights(Layout.vertical(Min(6), Length(6))))
        assertEquals(listOf(10, 0), heights(Layout.vertical(Length(12), Length(4)), Rect(0, 0, 1, 10)))
    }

    @Test
    fun `spacing separates segments and margin shrinks the split area`() {
        val rects = Layout.horizontal(Fill(1), Fill(1))
            .spacing(2)
            .margin(Padding.uniform(1))
            .split(area)

        assertEquals(listOf(Rect(3, 4, 8, 8), Rect(13, 4, 8, 8)), rects)
    }

    @Test
    fun `segments stay inside an area too small for the spacing`() {
        val rects = Layout.horizontal(Fill(1), Fill(1), Fill(1)).spacing(5).split(Rect(0, 0, 4, 1))

        rects.forEach { rect ->
            assertTrue(rect.x in 0..4 && rect.x + rect.width <= 4, "escaped the area: $rect")
        }
    }

    @Test
    fun `an empty layout has no segments`() {
        assertEquals(emptyList<Rect>(), Layout.vertical().split(area))
    }

    @Test
    fun `invalid constraints are rejected eagerly`() {
        assertThrows<IllegalArgumentException> { Length(-1) }
        assertThrows<IllegalArgumentException> { Percentage(101) }
        assertThrows<IllegalArgumentException> { Ratio(1, 0) }
        assertThrows<IllegalArgumentException> { Ratio(3, 2) }
        assertThrows<IllegalArgumentException> { Fill(-1) }
        assertThrows<IllegalArgumentException> { Layout.vertical(Fill(1)).spacing(-1) }
    }

    @Test
    fun `split follows a canvas area in local coordinates`() {
        val canvas = Canvas.offscreen(10, 4).region(3, 1, 6, 2)

        val (left, right) = Layout.horizontal(Length(2), Fill(1)).split(canvas.area)

        assertEquals(Rect(0, 0, 2, 2), left)
        assertEquals(Rect(2, 0, 4, 2), right)
    }
}
