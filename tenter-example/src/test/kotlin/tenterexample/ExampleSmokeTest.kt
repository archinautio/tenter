// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.MouseEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.archinaut.tenter.terminal.Terminal
import io.archinaut.tenter.widget.panel.Panel
import io.archinaut.tenter.widget.panel.PanelMode
import io.archinaut.tenter.widget.panel.PanelSet

internal class ExampleSmokeTest {

    @Test
    fun `headless consumer renders long list help and animation through public tenter seams`() {
        val result = runHeadlessSmoke()

        assertEquals(600, result.renderedRows)
        assertTrue(result.helpContainsMovement)
        assertTrue(result.animationCompleted)
    }

    @Test
    fun `validated keymap movement and public settled hit test drive selection and toggle`() {
        val app = ExampleApp()
        Terminal.headless(width = 140, height = 24).draw(app::render)

        app.handleKey(KeyboardEvent("ArrowDown"))
        app.handleKey(KeyboardEvent(" "))

        assertEquals(1, app.state.selectedRow)
        assertTrue(1 in app.state.checkedRows)

        val slot = checkNotNull(checkNotNull(app.panels.layout).slotOf(ExamplePanelId.ROWS))
        val clickX = slot.content.x + 1
        val clickY = slot.content.y + 3
        val hit = checkNotNull(app.panels.hitTest(clickX, clickY))
        val clickedRow = checkNotNull(hit.contentPoint).y

        app.handle(MouseEvent(x = clickX, y = clickY, left = true))

        assertEquals(clickedRow, app.state.selectedRow)
        assertTrue(clickedRow in app.state.checkedRows)
    }

    @Test
    fun `number keys focus a panel and plus or minus resize the focused one`() {
        val app = ExampleApp()

        app.handleKey(KeyboardEvent("2"))
        app.handleKey(KeyboardEvent("-"))

        assertEquals(ExamplePanelId.DETAILS, app.panels.focused)
        assertEquals(PanelMode.MINIMIZED, app.panels.modeOf(ExamplePanelId.DETAILS))

        app.handleKey(KeyboardEvent("1"))
        app.handleKey(KeyboardEvent("+"))

        assertEquals(PanelMode.MAXIMIZED, app.panels.modeOf(ExamplePanelId.ROWS))
    }

    @Test
    fun `the catalog, details, and checked panels each scroll by page and by wheel`() {
        val app = ExampleApp()
        repeat(30) {
            app.handleKey(KeyboardEvent(" "))
            app.handleKey(KeyboardEvent("ArrowDown"))
        }
        // Short enough that every panel's content overflows it.
        val terminal = Terminal.headless(width = 140, height = 9)
        terminal.draw(app::render)

        for ((key, id) in listOf("1" to ExamplePanelId.ROWS, "2" to ExamplePanelId.DETAILS, "3" to ExamplePanelId.CHECKED)) {
            app.handleKey(KeyboardEvent(key))
            app.handleKey(KeyboardEvent("PageDown"))
            terminal.draw(app::render)
            val paged = scrollOf(app, id)
            assertTrue(paged.offset.y > 0, "$id did not page down")

            val content = checkNotNull(checkNotNull(app.panels.layout).slotOf(id)).content
            app.handle(MouseEvent(x = content.x + 1, y = content.y + 1, wheelUp = true))
            terminal.draw(app::render)
            assertTrue(scrollOf(app, id).offset.y < paged.offset.y, "$id did not scroll up by wheel")
        }
    }

    @Test
    fun `arrow keys scroll a focused side panel a row and move the selection only in the catalog`() {
        val app = ExampleApp()
        repeat(30) {
            app.handleKey(KeyboardEvent(" "))
            app.handleKey(KeyboardEvent("ArrowDown"))
        }
        val terminal = Terminal.headless(width = 140, height = 9)
        terminal.draw(app::render)

        app.handleKey(KeyboardEvent("3"))
        app.handleKey(KeyboardEvent("ArrowDown"))
        app.handleKey(KeyboardEvent("ArrowDown"))
        terminal.draw(app::render)
        assertEquals(2, scrollOf(app, ExamplePanelId.CHECKED).offset.y)
        app.handleKey(KeyboardEvent("ArrowUp"))
        terminal.draw(app::render)
        assertEquals(1, scrollOf(app, ExamplePanelId.CHECKED).offset.y)
        assertEquals(30, app.state.selectedRow, "scrolling a side panel must not move the selection")

        app.handleKey(KeyboardEvent("1"))
        app.handleKey(KeyboardEvent("ArrowUp"))
        assertEquals(29, app.state.selectedRow)
    }

    private fun scrollOf(app: ExampleApp, id: ExamplePanelId) =
        checkNotNull(checkNotNull(checkNotNull(app.panels.layout).slotOf(id)).scroll)

    @Test
    fun `each app receives fresh panel instances and attachment remains exclusive`() {
        // Both apps coexist and render only because each one builds its own panel instances.
        val first = ExampleApp()
        val second = ExampleApp()
        Terminal.headless(width = 140, height = 24).draw(first::render)
        Terminal.headless(width = 140, height = 24).draw(second::render)

        assertEquals(ExamplePanelId.ROWS, first.panels.focused)
        assertEquals(ExamplePanelId.ROWS, second.panels.focused)

        // Sharing one instance across sets is what fails, and it fails at construction.
        val shared = Panel<ExamplePanelId, ExampleState>(
            id = ExamplePanelId.ROWS,
            title = "CATALOG",
            normal = { input -> ExampleListView(1, input) },
        )
        PanelSet(listOf(shared))

        assertThrows<IllegalArgumentException> { PanelSet(listOf(shared)) }
    }
}
