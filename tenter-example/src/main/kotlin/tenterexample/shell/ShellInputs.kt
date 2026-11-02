// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.shell

import io.archinaut.tenter.input.KeySection
import tenterexample.pages.PageView

/** What the panels are built from each frame: the page list, the [current] page's view, and the help. */
internal data class ShellInputs(
    val titles: List<String>,
    val current: Int,
    val page: PageView,
    val help: List<KeySection>,
)
