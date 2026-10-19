// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.palette.ChromeRole
import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Style

internal class PipTrackTest {

    private val used = Style(ChromeRole.DANGER)
    private val empty = Style(ChromeRole.TEXT_PRIMARY)

    private fun track(used: Int, capacity: Int, perRow: Int = 20, spacing: Int = 1): PipTrack = PipTrack(
        used = used,
        capacity = capacity,
        filled = "F",
        empty = "E",
        perRow = perRow,
        usedStyle = this.used,
        emptyStyle = empty,
        spacing = spacing,
    )

    private fun paint(content: MeasurableWidget): Buffer = render(content, 40, 10)

    @Test
    fun `draws used pips filled and the remainder empty`() {
        val buffer = paint(track(used = 3, capacity = 5))

        assertEquals("F F F E E", buffer.line(0))
    }

    @Test
    fun `styles filled and empty pips independently`() {
        val buffer = paint(track(used = 1, capacity = 2))

        assertEquals(ChromeRole.DANGER, buffer.get(0, 0).style.fg)
        assertEquals(ChromeRole.TEXT_PRIMARY, buffer.get(2, 0).style.fg)
    }

    @Test
    fun `wraps to a new row every perRow pips`() {
        val buffer = paint(track(used = 5, capacity = 7, perRow = 3))

        assertEquals("F F F", buffer.line(0))
        assertEquals("F F E", buffer.line(1))
        assertEquals("E", buffer.line(2))
    }

    @Test
    fun `clamps used above capacity`() {
        val buffer = paint(track(used = 99, capacity = 3))

        assertEquals("F F F", buffer.line(0))
    }

    @Test
    fun `height is the rows its capacity occupies, at least one`() {
        assertEquals(3, track(used = 0, capacity = 7, perRow = 3).measure(40).height)
        assertEquals(1, track(used = 0, capacity = 3, perRow = 3).measure(40).height)
        assertEquals(1, track(used = 0, capacity = 0, perRow = 3).measure(40).height)
    }

    @Test
    fun `a cursor advances past the rows used`() {
        val buffer = Buffer(40, 10)
        val cursor = TextCursor(Canvas.of(buffer))
        cursor.newLine()

        cursor.draw(track(used = 5, capacity = 7, perRow = 3))

        assertEquals("F F F", buffer.line(1))
        assertEquals("F F E", buffer.line(2))
        assertEquals(4, cursor.row)
    }

    @Test
    fun `offsets pips with padding`() {
        val buffer = paint(track(used = 1, capacity = 1).block(Block(padding = Padding(left = 5))))

        assertEquals("     F", buffer.line(0))
    }

    @Test
    fun `places two tracks side by side in columns`() {
        val buffer = paint(
            Columns(
                children = listOf(
                    Column(1, track(used = 1, capacity = 1)),
                    Column(1, track(used = 1, capacity = 1)),
                ),
                gutter = 3,
            ),
        )

        assertEquals("F   F", buffer.line(0))
    }

    @Test
    fun `rejects a non-positive perRow`() {
        assertThrows(IllegalArgumentException::class.java) { track(used = 0, capacity = 1, perRow = 0) }
    }

    @Test
    fun `rejects negative spacing`() {
        assertThrows(IllegalArgumentException::class.java) { track(used = 0, capacity = 1, perRow = 1, spacing = -1) }
    }
}
