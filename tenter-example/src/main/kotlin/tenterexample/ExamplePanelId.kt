// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import io.archinaut.tenter.widget.panel.PanelId

internal enum class ExamplePanelId : PanelId {
    /** The main panel: the generated catalog, at the left. */
    ROWS,

    /** The selected row's details, at the right. */
    DETAILS,

    /** Every checked row, at the right. */
    CHECKED,

    /** The key help, right-most. */
    HELP,
}
