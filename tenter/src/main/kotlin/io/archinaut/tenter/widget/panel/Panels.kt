// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.StatefulWidget

/**
 * Draws the panels of a [PanelSet] named in [areas], in its iteration order, each into its
 * rectangle in frame coordinates, building their content from [inputs]. A failed frame keeps the
 * set's previous [PanelSet.layout] and rolls nothing back.
 */
public class Panels<K : PanelId, I>(
    private val inputs: I,
    private val areas: Map<K, Rect>,
) : StatefulWidget<PanelSet<K, I>> {

    override fun render(canvas: Canvas, state: PanelSet<K, I>) {
        state.render(canvas, inputs, areas)
    }
}
