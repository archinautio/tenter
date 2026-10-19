// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.archinaut.tenter.widget.contentWidget

private enum class TestPanelId : PanelId { A, B }

internal class PanelModeTest {

    private fun panel(
        minimized: Boolean = true,
        maximized: Boolean = true,
    ): Panel<TestPanelId, Unit> = Panel(
        id = TestPanelId.A,
        title = "T",
        normal = { Panel.Presentation.fixedWidth(contentWidget { }, 20) },
        minimized = if (minimized) ({ Panel.Presentation.fixedWidth(contentWidget { }, Panel.MINIMIZED_WIDTH) }) else null,
        maximized = if (maximized) ({ Panel.Presentation.allocated(contentWidget { }) }) else null,
    )

    @Test
    fun `modes list exactly the declared modes in cycle order`() {
        assertEquals(
            listOf(PanelMode.MINIMIZED, PanelMode.NORMAL, PanelMode.MAXIMIZED),
            panel().modes,
        )
        assertEquals(listOf(PanelMode.NORMAL), panel(false, false).modes)
        assertEquals(listOf(PanelMode.MINIMIZED, PanelMode.NORMAL), panel(true, false).modes)
    }

    @Test
    fun `set operations cycle forward and backward and skip undeclared modes`() {
        val set = PanelSet.uniform(listOf(panel()))

        set.cycleFocusedMode(1)
        assertEquals(PanelMode.MAXIMIZED, set.modeOf(TestPanelId.A))
        set.cycleFocusedMode(1)
        assertEquals(PanelMode.MINIMIZED, set.modeOf(TestPanelId.A))
        set.cycleFocusedMode(-1)
        assertEquals(PanelMode.MAXIMIZED, set.modeOf(TestPanelId.A))

        val minimizedAndNormal = PanelSet.uniform(listOf(panel(minimized = true, maximized = false)))
        minimizedAndNormal.cycleFocusedMode(1)
        assertEquals(PanelMode.MINIMIZED, minimizedAndNormal.modeOf(TestPanelId.A))
    }

    @Test
    fun `maximized state restores to the state from which it was entered`() {
        val a = panel()
        val b = Panel<TestPanelId, Unit>(
            id = TestPanelId.B,
            title = "B",
            normal = { Panel.Presentation.fixedWidth(contentWidget { }, 20) },
        )
        val set = PanelSet.uniform(listOf(a, b))
        set.cycleFocusedMode(-1)
        set.cycleFocusedMode(-1)
        assertEquals(PanelMode.MAXIMIZED, set.modeOf(TestPanelId.A))
        set.focus(TestPanelId.B)
        assertEquals(PanelMode.MINIMIZED, set.modeOf(TestPanelId.A))
    }

    @Test
    fun `fixed presentation rejects nonpositive widths`() {
        for (width in listOf(-1, 0)) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
                Panel.Presentation.fixedWidth(contentWidget { }, width)
            }
        }
    }
}
