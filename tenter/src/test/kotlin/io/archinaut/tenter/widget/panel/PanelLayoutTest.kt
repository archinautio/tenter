// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.MeasurableWidget

private enum class LayoutPanelId : PanelId { A, B, C }

internal class PanelLayoutTest {

    private fun panel(id: LayoutPanelId): Panel<LayoutPanelId, Unit> = Panel(
        id = id,
        title = id.name,
        normal = { MeasurableWidget { writer -> repeat(40) { writer.writeLine("row$it") } } },
    )

    private fun render(
        areas: Map<LayoutPanelId, Rect>,
        canvas: Canvas = Canvas.of(Buffer(100, 30)),
    ): PanelLayout<LayoutPanelId> {
        val set = PanelSet(LayoutPanelId.entries.map(::panel))
        Panels(Unit, areas).render(canvas, set)
        return checkNotNull(set.layout)
    }

    @Test
    fun `each rendered panel is published at its area in paint order`() {
        val areas = linkedMapOf(
            LayoutPanelId.B to Rect(60, 0, 40, 30),
            LayoutPanelId.A to Rect(0, 0, 60, 30),
        )

        val layout = render(areas)

        assertEquals(listOf(LayoutPanelId.B, LayoutPanelId.A), layout.slots.map { it.id })
        assertEquals(Rect(60, 0, 40, 30), checkNotNull(layout.slotOf(LayoutPanelId.B)).outer)
        assertEquals(Rect(0, 0, 60, 30), checkNotNull(layout.slotOf(LayoutPanelId.A)).outer)
        assertNull(layout.slotOf(LayoutPanelId.C))
    }

    @Test
    fun `the content rectangle excludes the border and horizontal padding`() {
        val slot = render(mapOf(LayoutPanelId.A to Rect(10, 5, 30, 12))).slots.single()

        assertEquals(Rect(12, 6, 26, 10), slot.content)
    }

    @Test
    fun `areas are clipped to the canvas the panels render into`() {
        val layout = render(
            mapOf(
                LayoutPanelId.A to Rect(80, 0, 40, 30),
                LayoutPanelId.B to Rect(0, 40, 10, 10),
            ),
        )

        assertEquals(Rect(80, 0, 20, 30), checkNotNull(layout.slotOf(LayoutPanelId.A)).outer)
        val offscreen = checkNotNull(layout.slotOf(LayoutPanelId.B))
        assertEquals(0, offscreen.outer.height)
        assertNull(offscreen.scroll)
    }

    @Test
    fun `the last-painted panel owns overlapping cells`() {
        val layout = render(
            linkedMapOf(
                LayoutPanelId.A to Rect(0, 0, 100, 30),
                LayoutPanelId.C to Rect(20, 5, 30, 10),
            ),
        )

        assertEquals(LayoutPanelId.C, layout.panelAt(25, 8)?.id)
        assertEquals(LayoutPanelId.A, layout.panelAt(5, 8)?.id)
        assertNull(layout.panelAt(5, 30))
    }

    @Test
    fun `a published layout cannot be altered`() {
        val layout = render(mapOf(LayoutPanelId.A to Rect(0, 0, 50, 30)))

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException::class.java) {
            (layout.slots as MutableList).clear()
        }
    }
}
