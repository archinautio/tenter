// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.input.KeyHint
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.terminal.CompletedFrame

internal class KeyHelpTest {

    private fun renderHelp(sections: List<KeySection>, width: Int = 28, height: Int = 20): CompletedFrame =
        render(KeyHelp(sections), width, height)

    @Test
    fun `renders the KEYS header`() {
        val buffer = renderHelp(listOf(KeySection("MOVEMENT", listOf(KeyHint("Enter", "select unit")))))

        assertEquals("─── KEYS " + "─".repeat(19), buffer.region(Rect(0, 0, 28, 1)).line(0))
        assertEquals(io.archinaut.tenter.color.StandardRole.INFO, buffer.get(4, 0).fg)
    }

    @Test
    fun `renders each section's title and key-description rows`() {
        val buffer = renderHelp(
            listOf(
                KeySection("MOVEMENT", listOf(KeyHint("Enter", "select unit"))),
                KeySection("GLOBAL", listOf(KeyHint("ctrl+c", "quit"))),
            ),
        )

        assertEquals("MOVEMENT", buffer.region(Rect(0, 1, 28, 1)).line(0))
        val hintRow = buffer.region(Rect(0, 2, 28, 1)).line(0)
        assertTrue(hintRow.contains("Enter")) { "Expected key label, got: '$hintRow'" }
        assertTrue(hintRow.contains("select unit")) { "Expected description, got: '$hintRow'" }

        assertEquals("GLOBAL", buffer.region(Rect(0, 4, 28, 1)).line(0))
        val globalRow = buffer.region(Rect(0, 5, 28, 1)).line(0)
        assertTrue(globalRow.contains("ctrl+c")) { "Expected key label, got: '$globalRow'" }
        assertTrue(globalRow.contains("quit")) { "Expected description, got: '$globalRow'" }
    }

    @Test
    fun `blank row separates two sections`() {
        val buffer = renderHelp(
            listOf(
                KeySection("A", listOf(KeyHint("x", "one"))),
                KeySection("B", listOf(KeyHint("y", "two"))),
            ),
        )

        assertEquals("", buffer.region(Rect(0, 3, 28, 1)).line(0))
    }

    @Test
    fun `a single section renders with no trailing blank section gap`() {
        val buffer = renderHelp(listOf(KeySection("GLOBAL", listOf(KeyHint("ctrl+c", "quit")))))

        assertEquals("GLOBAL", buffer.region(Rect(0, 1, 28, 1)).line(0))
        assertEquals("", buffer.region(Rect(0, 3, 28, 1)).line(0))
    }

    @Test
    fun `long descriptions wrap with a hanging indent under the key column`() {
        val longDescription = "this description is deliberately long enough that it must wrap onto a second line"
        val buffer = renderHelp(
            listOf(KeySection("GLOBAL", listOf(KeyHint("alt+0-5", longDescription)))),
            width = 24,
        )

        val firstLine = buffer.region(Rect(0, 2, 24, 1)).line(0)
        val secondLine = buffer.region(Rect(0, 3, 24, 1)).line(0)
        assertTrue(firstLine.contains("alt+0-5")) { "Expected key label on first line, got: '$firstLine'" }
        assertTrue(secondLine.isNotEmpty()) { "Expected a wrapped continuation line" }
        // Continuation text starts under the key column (after the "key + 2 spaces" prefix),
        // not flush against the left edge.
        assertEquals("", secondLine.take(9).trimEnd())
    }
}
