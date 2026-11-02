// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.shell

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.input.HintGroup
import io.archinaut.tenter.input.KeyBinding
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeyMap

internal object ShellKeyMap {
    internal val map: KeyMap<ShellContext, ShellAction> = KeyMap(
        mapOf(
            ShellContext.GLOBAL to KeyLayer(
                title = "SHOWCASE",
                bindings = listOf(
                    KeyBinding(KeyboardEvent("Tab"), ShellAction.NEXT_FOCUS, "focus"),
                    KeyBinding(KeyboardEvent("Tab", shift = true), ShellAction.PREVIOUS_FOCUS, "focus"),
                    KeyBinding(KeyboardEvent("]"), ShellAction.NEXT_PAGE, "page"),
                    KeyBinding(KeyboardEvent("["), ShellAction.PREVIOUS_PAGE, "page"),
                    KeyBinding(KeyboardEvent("+"), ShellAction.GROW_PANEL, "resize"),
                    KeyBinding(KeyboardEvent("-"), ShellAction.SHRINK_PANEL, "resize"),
                    KeyBinding(KeyboardEvent("q"), ShellAction.QUIT, "quit"),
                ),
                hintGroups = listOf(
                    HintGroup("focus", "Tab/⇧Tab", "focus the next or previous panel"),
                    HintGroup("page", "[/]", "show the previous or next page"),
                    HintGroup("resize", "+/-", "step the focused panel through its modes"),
                    HintGroup("click", "click", "focus a panel; choose a page", bindingless = true),
                    HintGroup("quit", "q", "quit the showcase"),
                ),
            ),
            ShellContext.PAGES to KeyLayer(
                title = "PAGES",
                bindings = listOf(
                    KeyBinding(KeyboardEvent("ArrowUp"), ShellAction.PAGE_ABOVE, "choose"),
                    KeyBinding(KeyboardEvent("ArrowDown"), ShellAction.PAGE_BELOW, "choose"),
                    KeyBinding(KeyboardEvent("Enter"), ShellAction.OPEN_PAGE, "open"),
                ),
                hintGroups = listOf(
                    HintGroup("choose", "↑/↓", "choose a page"),
                    HintGroup("open", "Enter", "focus the page"),
                ),
            ),
            ShellContext.SCROLL to KeyLayer(
                title = "SCROLL",
                bindings = listOf(
                    KeyBinding(KeyboardEvent("ArrowUp"), ShellAction.SCROLL_UP, "line"),
                    KeyBinding(KeyboardEvent("ArrowDown"), ShellAction.SCROLL_DOWN, "line"),
                    KeyBinding(KeyboardEvent("PageUp"), ShellAction.PAGE_UP, "page"),
                    KeyBinding(KeyboardEvent("PageDown"), ShellAction.PAGE_DOWN, "page"),
                ),
                hintGroups = listOf(
                    HintGroup("line", "↑/↓", "scroll the focused panel a row"),
                    HintGroup("page", "PgUp/PgDn", "scroll the focused panel a page"),
                    HintGroup("wheel", "wheel", "scroll the panel under the pointer", bindingless = true),
                ),
            ),
        ),
    )
}
