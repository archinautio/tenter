// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.widget.CheckState
import io.archinaut.tenter.widget.Checkbox
import io.archinaut.tenter.widget.CheckboxGlyphs
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.Row
import io.archinaut.tenter.widget.SelectableRow
import io.archinaut.tenter.widget.Stack
import kotlin.time.Duration
import tenterexample.pages.PageSections.heading
import tenterexample.pages.PageSections.note

/** A checklist of selectable rows with a tri-state header, standalone checkboxes, and glyph choices. */
internal class SelectionPage : ShowcasePage<SelectionPage.State, SelectionPage.Action> {

    internal data class State(
        val selected: Int = 0,
        val checks: List<CheckState> = TASKS.map { CheckState.UNCHECKED },
        val glyphs: Int = 0,
    )

    internal enum class Action { UP, DOWN, TOGGLE, PARTIAL, ALL, GLYPHS }

    override val title: String = "Selection"

    override val initialState: State = State()

    private val keys = PageKeys(
        KeyLayer(
            title = "SELECTION",
            bindings = listOf(
                KeyBinding(KeyboardEvent("ArrowUp"), Action.UP, "move"),
                KeyBinding(KeyboardEvent("ArrowDown"), Action.DOWN, "move"),
                KeyBinding(KeyboardEvent(" "), Action.TOGGLE, "toggle"),
                KeyBinding(KeyboardEvent("i"), Action.PARTIAL, "partial"),
                KeyBinding(KeyboardEvent("a"), Action.ALL, "all"),
                KeyBinding(KeyboardEvent("g"), Action.GLYPHS, "glyphs"),
            ),
            hintGroups = listOf(
                HintGroup("move", "↑/↓", "move the selection"),
                HintGroup("toggle", "space", "check or uncheck the selected task"),
                HintGroup("partial", "i", "mark the selected task indeterminate"),
                HintGroup("all", "a", "check every task, or none"),
                HintGroup("glyphs", "g", "cycle the checkbox glyphs"),
                HintGroup("click", "click", "select and toggle a task", bindingless = true),
            ),
        ),
    )

    override fun resolve(state: State, event: KeyboardEvent): Action? = keys.resolve(event)

    override fun hints(state: State): List<KeySection> = keys.hints

    override fun handle(state: State, action: Action, now: Duration): State = when (action) {
        Action.UP -> state.copy(selected = (state.selected - 1).coerceAtLeast(0))
        Action.DOWN -> state.copy(selected = (state.selected + 1).coerceAtMost(TASKS.lastIndex))
        Action.TOGGLE -> state.toggled(state.selected)
        Action.PARTIAL -> state.copy(checks = state.checks.replaced(state.selected, CheckState.INDETERMINATE))
        Action.ALL -> {
            val next = if (state.checks.all { it == CheckState.CHECKED }) CheckState.UNCHECKED else CheckState.CHECKED
            state.copy(checks = state.checks.map { next })
        }
        Action.GLYPHS -> state.copy(glyphs = (state.glyphs + 1) % GLYPHS.size)
    }

    override fun click(state: State, point: Point, now: Duration): State {
        val task = point.y - FIRST_TASK_ROW
        if (task !in TASKS.indices) return state
        return state.copy(selected = task).toggled(task)
    }

    override fun view(state: State, now: Duration): PageView {
        val (glyphName, glyphs) = GLYPHS[state.glyphs]
        val done = state.checks.count { it == CheckState.CHECKED }
        val header = SelectableRow(
            label = "all tasks",
            checkState = aggregate(state.checks),
            selected = false,
            right = "$done/${TASKS.size}",
            glyphs = glyphs,
        )
        // --8<-- [start:rows]
        val rows = TASKS.mapIndexed { index, (label, size) ->
            SelectableRow(
                label = label,
                checkState = state.checks[index],
                selected = index == state.selected,
                right = size,
                glyphs = glyphs,
            )
        }
        // --8<-- [end:rows]
        val checkboxes = CheckState.entries.map { checkState ->
            Row(
                listOf(
                    Constraint.Length(2) to Checkbox(checkState, glyphs = glyphs),
                    Constraint.Fill(1) to Paragraph(checkState.name.lowercase()),
                ),
            )
        }
        return PageView(
            Stack(
                listOf(
                    // The checklist comes first, so a click's row maps straight to a task.
                    Stack(listOf(heading("SelectableRow, $glyphName glyphs"), header) + rows, spacing = 0),
                    heading("Checkbox"),
                    Stack(checkboxes, spacing = 0),
                    note(
                        "The header is indeterminate while only some tasks are checked. Nerd Font glyphs " +
                            "need a patched font; ASCII suits the most conservative terminals.",
                    ),
                ),
            ),
        )
    }

    private fun aggregate(checks: List<CheckState>): CheckState = when {
        checks.all { it == CheckState.CHECKED } -> CheckState.CHECKED
        checks.all { it == CheckState.UNCHECKED } -> CheckState.UNCHECKED
        else -> CheckState.INDETERMINATE
    }

    private fun State.toggled(task: Int): State {
        val next = if (checks[task] == CheckState.CHECKED) CheckState.UNCHECKED else CheckState.CHECKED
        return copy(checks = checks.replaced(task, next))
    }

    private fun List<CheckState>.replaced(index: Int, value: CheckState): List<CheckState> =
        mapIndexed { i, old -> if (i == index) value else old }

    private companion object {
        /** The heading and the header row sit above the first task. */
        private const val FIRST_TASK_ROW: Int = 2

        private val TASKS: List<Pair<String, String>> = listOf(
            "Download the sources" to "12 MB",
            "Verify checksums" to "0.4 s",
            "Compile the library" to "38 s",
            "Run the tests" to "1 m 12 s",
            "Build the documentation" to "9 s",
            "Publish to Maven Local" to "2 s",
        )

        private val GLYPHS: List<Pair<String, CheckboxGlyphs>> = listOf(
            "default" to CheckboxGlyphs.DEFAULT,
            "ASCII" to CheckboxGlyphs.ASCII,
            "Nerd Font" to CheckboxGlyphs.NERD_FONT,
        )
    }
}
