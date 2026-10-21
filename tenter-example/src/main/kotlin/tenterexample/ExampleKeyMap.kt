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
                    KeyBinding(KeyboardEvent("1"), ExampleAction.FOCUS_CATALOG, "focus"),
                    KeyBinding(KeyboardEvent("2"), ExampleAction.FOCUS_DETAILS, "focus"),
                    KeyBinding(KeyboardEvent("3"), ExampleAction.FOCUS_CHECKED, "focus"),
                    KeyBinding(KeyboardEvent("h"), ExampleAction.FOCUS_HELP, "focus"),
                    KeyBinding(KeyboardEvent("+"), ExampleAction.GROW_PANEL, "resize"),
                    KeyBinding(KeyboardEvent("-"), ExampleAction.SHRINK_PANEL, "resize"),
                    KeyBinding(KeyboardEvent("PageUp"), ExampleAction.PAGE_UP, "scroll"),
                    KeyBinding(KeyboardEvent("PageDown"), ExampleAction.PAGE_DOWN, "scroll"),
                    KeyBinding(KeyboardEvent("q"), ExampleAction.QUIT, "quit"),
                ),
                hintGroups = listOf(
                    HintGroup("movement", "↑/↓", "move through the generated rows"),
                    HintGroup("lineScroll", "↑/↓", "scroll the focused side panel a row"),
                    HintGroup("toggle", "space", "toggle the selected row"),
                    HintGroup("focus", "1/2/3/h", "focus catalog, details, checked, or help"),
                    HintGroup("resize", "+/-", "maximize or minimize the focused panel"),
                    HintGroup("scroll", "PgUp/PgDn", "manually scroll the focused panel"),
                    HintGroup("quit", "q", "quit the example"),
                ),
            ),
            // Sectionless: its rows are documented by the LIST section's "lineScroll" group.
            ExampleContext.PANEL_SCROLL to KeyLayer(
                title = null,
                bindings = listOf(
                    KeyBinding(KeyboardEvent("ArrowUp"), ExampleAction.SCROLL_UP, "lineScroll"),
                    KeyBinding(KeyboardEvent("ArrowDown"), ExampleAction.SCROLL_DOWN, "lineScroll"),
                ),
            ),
        ),
    )
}
