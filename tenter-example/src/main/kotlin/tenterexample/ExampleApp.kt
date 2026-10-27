// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.InputEvent
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.MouseEvent
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.widget.panel.Panel
import io.archinaut.tenter.widget.panel.Panels
import io.archinaut.tenter.widget.panel.PanelSet
import io.archinaut.tenter.widget.panel.PanelMode
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Layout
import io.archinaut.tenter.screen.Padding
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.terminal.Frame
import io.archinaut.tenter.widget.Block
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.KeyHelp
import io.archinaut.tenter.widget.Stack
import io.archinaut.tenter.widget.VerticalText
import io.archinaut.tenter.widget.Widget

/**
 * Small application-owned controller showing how a consumer dispatches toolkit observations. Its
 * screen is a status bar across the top, the catalog as the main panel at the left, and the
 * details, checked, and help panels in fixed-width columns at the right.
 */
internal class ExampleApp {
    internal val panels: PanelSet<ExamplePanelId, ExampleState> = createPanelSet()
    internal var state: ExampleState = ExampleState()
        private set
    internal var running: Boolean = true
        private set

    internal fun render(frame: Frame) {
        // --8<-- [start:panel-render]
        val (statusArea, body) = Layout.vertical(Constraint.Length(STATUS_HEIGHT), Constraint.Fill(1)).split(frame.area)
        frame.renderWidget(StatusBar(state, panels.focused), statusArea)
        frame.renderStatefulWidget(Panels(state, panelAreas(body)), body, panels)
        // --8<-- [end:panel-render]
    }

    // --8<-- [start:panel-layout]
    private fun panelAreas(area: Rect): Map<ExamplePanelId, Rect> {
        // A maximized panel takes the whole area alone.
        ExamplePanelId.entries.firstOrNull { panels.modeOf(it) == PanelMode.MAXIMIZED }
            ?.let { return mapOf(it to area) }

        // The catalog fills whatever the right-hand columns leave, each at its mode's width.
        val sides = listOf(ExamplePanelId.DETAILS, ExamplePanelId.CHECKED, ExamplePanelId.HELP)
        val widths = sides.map { id ->
            Constraint.Length(if (panels.modeOf(id) == PanelMode.MINIMIZED) MINIMIZED_WIDTH else SIDE_WIDTHS.getValue(id))
        }
        val rects = Layout.horizontal(listOf(Constraint.Fill(1)) + widths).split(area)
        return (listOf(ExamplePanelId.ROWS) + sides).zip(rects).toMap()
    }
    // --8<-- [end:panel-layout]

    internal fun handle(event: InputEvent) {
        when (event) {
            is KeyboardEvent -> handleKey(event)
            is MouseEvent -> handleMouse(event)
        }
    }

    internal fun handleKey(event: KeyboardEvent) {
        // A focused side panel takes ↑/↓ for scrolling; the catalog keeps them for the selection.
        val contexts = if (panels.focused == ExamplePanelId.ROWS) {
            listOf(ExampleContext.LIST)
        } else {
            listOf(ExampleContext.PANEL_SCROLL, ExampleContext.LIST)
        }
        when (ExampleKeyMap.map.resolve(contexts, event)) {
            ExampleAction.MOVE_UP -> moveSelection(-1)
            ExampleAction.MOVE_DOWN -> moveSelection(1)
            ExampleAction.TOGGLE -> toggleSelected()
            ExampleAction.FOCUS_CATALOG -> panels.focus(ExamplePanelId.ROWS)
            ExampleAction.FOCUS_DETAILS -> panels.focus(ExamplePanelId.DETAILS)
            ExampleAction.FOCUS_CHECKED -> panels.focus(ExamplePanelId.CHECKED)
            ExampleAction.FOCUS_HELP -> panels.focus(ExamplePanelId.HELP)
            ExampleAction.GROW_PANEL -> panels.cycleFocusedMode(1)
            ExampleAction.SHRINK_PANEL -> panels.cycleFocusedMode(-1)
            ExampleAction.QUIT -> running = false
            ExampleAction.PAGE_UP -> panels.page(panels.focused, -1)
            ExampleAction.PAGE_DOWN -> panels.page(panels.focused, 1)
            ExampleAction.SCROLL_UP -> panels.scroll(panels.focused, 0, -1)
            ExampleAction.SCROLL_DOWN -> panels.scroll(panels.focused, 0, 1)
            null -> Unit
        }
    }

    internal fun handleMouse(event: MouseEvent) {
        // --8<-- [start:mouse]
        val hit = panels.hitTest(event.x, event.y)
        val scrollDelta = when {
            event.wheelUp -> -WHEEL_STEP
            event.wheelDown -> WHEEL_STEP
            else -> null
        }
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

    /** Focuses [id] and steps it to its next declared mode, returning the mode it lands in. */
    internal fun cycle(id: ExamplePanelId): PanelMode {
        panels.focus(id)
        panels.cycleFocusedMode(1)
        return panels.modeOf(id)
    }

    private fun moveSelection(delta: Int) {
        state = state.copy(selectedRow = (state.selectedRow + delta).coerceIn(0, EXAMPLE_ROW_COUNT - 1))
        panels.focus(ExamplePanelId.ROWS)
    }

    private fun toggleSelected() {
        state = state.copy(checkedRows = state.checkedRows.toggle(state.selectedRow))
        panels.focus(ExamplePanelId.ROWS)
    }

    private fun createPanelSet(): PanelSet<ExamplePanelId, ExampleState> {
        // --8<-- [start:panels]
        // The main panel declares no minimized content, so it only toggles maximized.
        val rows = Panel<ExamplePanelId, ExampleState>(
            id = ExamplePanelId.ROWS,
            title = "CATALOG",
            badge = "1",
            normal = { input -> ExampleListView(EXAMPLE_ROW_COUNT, input) },
            maximized = { input -> ExampleListView(EXAMPLE_ROW_COUNT, input) },
        )
        val details = Panel<ExamplePanelId, ExampleState>(
            id = ExamplePanelId.DETAILS,
            title = "DETAILS",
            badge = "2",
            normal = { input -> detailsContent(input) },
            minimized = { VerticalText("DETAILS") },
            maximized = { input -> detailsContent(input) },
        )
        val checked = Panel<ExamplePanelId, ExampleState>(
            id = ExamplePanelId.CHECKED,
            title = "CHECKED",
            badge = "3",
            normal = { input -> checkedContent(input) },
            minimized = { VerticalText("CHECKED") },
            maximized = { input -> checkedContent(input) },
        )
        val help = Panel<ExamplePanelId, ExampleState>(
            id = ExamplePanelId.HELP,
            title = "HELP",
            badge = "H",
            normal = { helpContent() },
            maximized = { helpContent() },
        )
        return PanelSet(listOf(rows, details, checked, help))
        // --8<-- [end:panels]
    }

    private fun detailsContent(input: ExampleState): MeasurableWidget {
        val isChecked = input.selectedRow in input.checkedRows
        return Stack(
            listOf(
                Paragraph(exampleRowLabel(input.selectedRow), Style(StandardRole.ACCENT), wrap = true),
                Paragraph(
                    if (isChecked) "checked" else "not checked",
                    Style(if (isChecked) StandardRole.SUCCESS else StandardRole.TEXT_MUTED),
                ),
            ),
        )
    }

    private fun checkedContent(input: ExampleState): MeasurableWidget =
        if (input.checkedRows.isEmpty()) {
            Paragraph("nothing checked yet", Style(StandardRole.TEXT_MUTED))
        } else {
            Paragraph(input.checkedRows.sorted().joinToString("\n") { "row $it" })
        }

    private fun helpContent(): MeasurableWidget = KeyHelp(listOf(ExampleKeyMap.map.hints(ExampleContext.LIST)))

    /** The status bar across the top: which panel has focus, the selection, and the checked count. */
    private class StatusBar(private val state: ExampleState, private val focused: ExamplePanelId) : Widget {
        override fun render(canvas: Canvas) {
            val block = Block.bordered(padding = Padding.horizontal(1))
            block.render(canvas)
            val text = "focus: ${focused.name}  •  selected: row ${state.selectedRow}  •  checked: ${state.checkedRows.size}"
            Paragraph(text, Style(StandardRole.TEXT_PRIMARY)).render(block.inner(canvas))
        }
    }

    internal companion object {
        /** Rows taken by the status bar: its border and one line of text. */
        internal const val STATUS_HEIGHT: Int = 3
        internal const val MINIMIZED_WIDTH: Int = 7

        /** Each right-hand panel's width in its normal mode. */
        internal val SIDE_WIDTHS: Map<ExamplePanelId, Int> = mapOf(
            ExamplePanelId.DETAILS to 24,
            ExamplePanelId.CHECKED to 18,
            ExamplePanelId.HELP to 36,
        )

        private const val WHEEL_STEP: Int = 2

        private fun Set<Int>.toggle(value: Int): Set<Int> = if (value in this) this - value else this + value
    }
}
