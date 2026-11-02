// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.shell

import io.archinaut.tenter.widget.panel.PanelId

/** The showcase's panels: the page list, one demo panel per page, and the key help. */
internal sealed interface ShellPanelId : PanelId {
    /** The list of pages, at the left. */
    data object Pages : ShellPanelId

    /** Page [page]'s content, filling the middle; each page has its own, so it keeps its scroll and mode. */
    data class Demo(val page: Int) : ShellPanelId

    /** The keys of the focused panel and the current page, at the right. */
    data object Help : ShellPanelId
}
