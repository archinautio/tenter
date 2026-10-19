// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.InputEvent
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.MouseEvent
import io.archinaut.tenter.input.MouseInput
import io.archinaut.tenter.input.ScrollAction
import io.archinaut.tenter.widget.panel.Panel
import io.archinaut.tenter.widget.panel.Panels
import io.archinaut.tenter.widget.panel.PanelSet
import io.archinaut.tenter.widget.panel.PanelMode
import io.archinaut.tenter.terminal.Frame
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.KeyHelp
import io.archinaut.tenter.widget.Stack

internal enum class ExampleLayoutMode {
    MAIN_AND_SIDES,
    UNIFORM,
}

/** Small application-owned controller showing how a consumer dispatches toolkit observations. */
internal class ExampleApp(
    private val layoutMode: ExampleLayoutMode,
) {
    internal val panels: PanelSet<ExamplePanelId, ExampleState> = createPanelSet(layoutMode)
    internal var state: ExampleState = ExampleState()
        private set
    internal var running: Boolean = true
        private set

    internal fun render(frame: Frame) {
        // --8<-- [start:panel-render]
        frame.renderStatefulWidget(Panels(state, VISIBLE), frame.area, panels)
        // --8<-- [end:panel-render]
    }

    internal fun handle(event: InputEvent) {
        when (event) {
            is KeyboardEvent -> handleKey(event)
            is MouseEvent -> handleMouse(event)
        }
    }

    internal fun handleKey(event: KeyboardEvent) {
        when (val action = ExampleKeyMap.map.resolve(listOf(ExampleContext.LIST), event)) {
            ExampleAction.MOVE_UP -> moveSelection(-1)
            ExampleAction.MOVE_DOWN -> moveSelection(1)
            ExampleAction.TOGGLE -> toggleSelected()
            ExampleAction.FOCUS_HELP -> panels.focus(ExamplePanelId.HELP)
            ExampleAction.CYCLE_PANEL -> {
                panels.focus(ExamplePanelId.HELP)
                panels.cycleFocusedMode(1)
            }
            ExampleAction.QUIT -> running = false
            is ScrollAction.Lines -> panels.scrollFocused(0, action.delta)
            is ScrollAction.Pages -> panels.pageFocused(action.delta)
            null -> Unit
        }
    }

    internal fun handleMouse(event: MouseEvent) {
        // --8<-- [start:mouse]
        val hit = panels.hitTest(event.x, event.y)
        val scrollDelta = MouseInput.scrollDelta(event)
        if (scrollDelta != null) {
            hit?.let { panels.scroll(it.id, 0, scrollDelta) }
            return
        }
        if (!event.left || hit?.id != ExamplePanelId.ROWS) return
        val contentPoint = hit.contentPoint ?: return
        if (contentPoint.y !in 0 until EXAMPLE_ROW_COUNT) return

        state = state.copy(
            selectedRow = contentPoint.y,
            checkedRows = state.checkedRows.toggle(contentPoint.y),
        )
        panels.focus(ExamplePanelId.ROWS)
        // --8<-- [end:mouse]
    }

    internal fun cycleHelp(): PanelMode? {
        panels.focus(ExamplePanelId.HELP)
        panels.cycleFocusedMode(1)
        return panels.modeOf(ExamplePanelId.HELP)
    }

    private fun moveSelection(delta: Int) {
        state = state.copy(selectedRow = (state.selectedRow + delta).coerceIn(0, EXAMPLE_ROW_COUNT - 1))
        panels.focus(ExamplePanelId.ROWS)
    }

    private fun toggleSelected() {
        state = state.copy(checkedRows = state.checkedRows.toggle(state.selectedRow))
        panels.focus(ExamplePanelId.ROWS)
    }

    private fun createPanelSet(mode: ExampleLayoutMode): PanelSet<ExamplePanelId, ExampleState> {
        // --8<-- [start:panels]
        val rows = Panel<ExamplePanelId, ExampleState>(
            id = ExamplePanelId.ROWS,
            title = "CATALOG",
            badge = "R",
            normal = { input -> Panel.Presentation.allocated(ExampleListView(EXAMPLE_ROW_COUNT, input)) },
        )
        val help = Panel<ExamplePanelId, ExampleState>(
            id = ExamplePanelId.HELP,
            title = "HELP",
            badge = "H",
            normal = { input -> Panel.Presentation.fixedWidth(helpContent(input), width = HELP_WIDTH) },
            minimized = { input -> Panel.Presentation.fixedWidth(helpContent(input), width = Panel.MINIMIZED_WIDTH) },
            maximized = { input -> Panel.Presentation.allocated(helpContent(input)) },
        )
        // --8<-- [end:panels]
        return when (mode) {
            ExampleLayoutMode.MAIN_AND_SIDES -> PanelSet.mainAndSides(rows, listOf(help))
            ExampleLayoutMode.UNIFORM -> PanelSet.uniform(
                listOf(rows, help), reservedColumns = 2, fixedWidthPanels = setOf(ExamplePanelId.HELP),
            )
        }
    }

    private fun helpContent(input: ExampleState): MeasurableWidget = Stack(
        listOf(
            KeyHelp(listOf(ExampleKeyMap.map.hints(ExampleContext.LIST))),
            Paragraph("\nSelected: row ${input.selectedRow}\nChecked: ${input.checkedRows.size}"),
        ),
    )

    private companion object {
        private const val HELP_WIDTH: Int = 36
        private val VISIBLE: Set<ExamplePanelId> = setOf(ExamplePanelId.ROWS, ExamplePanelId.HELP)

        private fun Set<Int>.toggle(value: Int): Set<Int> = if (value in this) this - value else this + value
    }
}
