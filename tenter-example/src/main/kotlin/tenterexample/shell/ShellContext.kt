// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.shell

/** The showcase's own key contexts; the current page's come between [GLOBAL] and the others. */
internal enum class ShellContext {
    /** Active everywhere, ahead of everything else. */
    GLOBAL,

    /** Choosing a page, while the page list has focus. */
    PAGES,

    /** Scrolling the focused panel, after the page's own keys. */
    SCROLL,
}
