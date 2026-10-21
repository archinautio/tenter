// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

internal enum class ExampleContext {
    LIST,

    /** ↑/↓ scroll the focused side panel; active, ahead of [LIST], while one has focus. */
    PANEL_SCROLL,
}
