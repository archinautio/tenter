// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import io.archinaut.tenter.screen.Point

/** The panel at a screen coordinate in the last completed frame; see [PanelSet.hitTest]. */
public data class PanelHit<K : PanelId>(
    public val id: K,
    /** The point in the content's own coordinates, or null for a border or padding hit. */
    public val contentPoint: Point?,
)
