// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeyMap
import io.archinaut.tenter.input.KeySection
import io.archinaut.tenter.screen.Line
import io.archinaut.tenter.screen.Span
import io.archinaut.tenter.screen.Style
import io.archinaut.tenter.widget.Paragraph
import io.archinaut.tenter.widget.Stack
import kotlin.time.Duration
import tenterexample.pages.PageSections.heading
import tenterexample.pages.PageSections.note

/**
 * A cursor moved through a key map with two contexts: arrows always, and a VI layer the user turns
 * on ahead of them. The help panel follows whichever contexts are active.
 */
internal class KeysPage : ShowcasePage<KeysPage.State, KeysPage.Action> {

    internal data class State(
        val x: Int = 0,
        val y: Int = 0,
        val vi: Boolean = false,
    )

    internal enum class Context { ARROWS, VI }

    internal enum class Action { LEFT, RIGHT, UP, DOWN, TOGGLE_VI }

    override val title: String = "Keys"

    override val initialState: State = State()

    private val map: KeyMap<Context, Action> = KeyMap(
        mapOf(
            Context.ARROWS to KeyLayer(
                title = "ARROWS",
                bindings = listOf(
                    KeyBinding(KeyboardEvent("ArrowLeft"), Action.LEFT, "move"),
                    KeyBinding(KeyboardEvent("ArrowRight"), Action.RIGHT, "move"),
                    KeyBinding(KeyboardEvent("ArrowUp"), Action.UP, "move"),
                    KeyBinding(KeyboardEvent("ArrowDown"), Action.DOWN, "move"),
                    KeyBinding(KeyboardEvent("v"), Action.TOGGLE_VI, "vi"),
                ),
                hintGroups = listOf(
                    HintGroup("move", "←→↑↓", "move the cursor"),
                    HintGroup("vi", "v", "turn the VI context on or off"),
                ),
            ),
            Context.VI to KeyLayer(
                title = "VI",
                bindings = listOf(
                    KeyBinding(KeyboardEvent("h"), Action.LEFT, "move"),
                    KeyBinding(KeyboardEvent("l"), Action.RIGHT, "move"),
                    KeyBinding(KeyboardEvent("k"), Action.UP, "move"),
                    KeyBinding(KeyboardEvent("j"), Action.DOWN, "move"),
                ),
                hintGroups = listOf(HintGroup("move", "h/j/k/l", "move the cursor")),
            ),
        ),
    )

    override fun resolve(state: State, event: KeyboardEvent): Action? = map.resolve(active(state), event)

    override fun hints(state: State): List<KeySection> = active(state).map(map::hints)

    override fun handle(state: State, action: Action, now: Duration): State = when (action) {
        Action.LEFT -> state.copy(x = (state.x - 1).coerceAtLeast(0))
        Action.RIGHT -> state.copy(x = (state.x + 1).coerceAtMost(GRID_WIDTH - 1))
        Action.UP -> state.copy(y = (state.y - 1).coerceAtLeast(0))
        Action.DOWN -> state.copy(y = (state.y + 1).coerceAtMost(GRID_HEIGHT - 1))
        Action.TOGGLE_VI -> state.copy(vi = !state.vi)
    }

    override fun view(state: State, now: Duration): PageView {
        val grid = (0 until GRID_HEIGHT).map { y ->
            Line(
                (0 until GRID_WIDTH).flatMap { x ->
                    val cursor = x == state.x && y == state.y
                    listOf(
                        Span(if (cursor) "●" else "·", Style(if (cursor) StandardRole.ACCENT else StandardRole.TEXT_SUBTLE)),
                        Span(" "),
                    )
                },
            )
        }
        val bindings = Action.entries.map { action ->
            Line(
                Span(action.name.padEnd(ACTION_WIDTH), Style(StandardRole.INFO)),
                Span(map.chordsFor(action).joinToString { it.key }),
            )
        }
        return PageView(
            Stack(
                listOf(
                    heading("Active contexts: ${active(state).joinToString()}"),
                    Paragraph(grid),
                    heading("Chords bound to each action"),
                    Paragraph(bindings),
                    note(
                        "The showcase resolves its own SHOWCASE keys first, then this page's contexts in " +
                            "order, then scrolling: the first context that binds a chord wins. The help " +
                            "panel lists the sections of the contexts active now.",
                    ),
                ),
            ),
        )
    }

    private fun active(state: State): List<Context> = if (state.vi) listOf(Context.VI, Context.ARROWS) else listOf(Context.ARROWS)

    private companion object {
        private const val GRID_WIDTH: Int = 12
        private const val GRID_HEIGHT: Int = 5
        private const val ACTION_WIDTH: Int = 11
    }
}
