// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeyMap

internal object ExampleKeyMap {
    internal val map: KeyMap<ExampleContext, ExampleAction> = KeyMap(
        mapOf(
            ExampleContext.LIST to KeyLayer(
                title = "LIST",
                bindings = listOf(
                    KeyBinding(KeyboardEvent("ArrowUp"), ExampleAction.MOVE_UP, "movement"),
                    KeyBinding(KeyboardEvent("ArrowDown"), ExampleAction.MOVE_DOWN, "movement"),
                    KeyBinding(KeyboardEvent(" "), ExampleAction.TOGGLE, "toggle"),
                    KeyBinding(KeyboardEvent("h"), ExampleAction.FOCUS_HELP, "panels"),
                    KeyBinding(KeyboardEvent("m"), ExampleAction.CYCLE_PANEL, "panels"),
                    KeyBinding(KeyboardEvent("PageUp"), ExampleAction.PAGE_UP, "scroll"),
                    KeyBinding(KeyboardEvent("PageDown"), ExampleAction.PAGE_DOWN, "scroll"),
                    KeyBinding(KeyboardEvent("q"), ExampleAction.QUIT, "quit"),
                ),
                hintGroups = listOf(
                    HintGroup("movement", "↑/↓", "move through the generated rows"),
                    HintGroup("toggle", "space", "toggle the selected row"),
                    HintGroup("panels", "h/m", "focus help or cycle its size"),
                    HintGroup("scroll", "PgUp/PgDn", "manually scroll the focused panel"),
                    HintGroup("quit", "q", "quit the example"),
                ),
            ),
        ),
    )
}
