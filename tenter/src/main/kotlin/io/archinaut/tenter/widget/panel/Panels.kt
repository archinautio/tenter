// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.widget.StatefulWidget

/**
 * Draws panels of a [PanelSet], each into its rectangle of [areas], building each panel's content
 * from [inputs], with the set as its state. Read the completed frame's geometry from
 * [PanelSet.layout] afterwards.
 *
 * The application owns the layout: split the frame with [io.archinaut.tenter.screen.Layout] each
 * frame, choosing every panel's size from its [PanelSet.modeOf] — for example, giving a maximized
 * panel the whole area and leaving the others out. Only the panels in [areas] are drawn, in its
 * iteration order, so a later one paints over an earlier one where they overlap. Areas are frame
 * coordinates, like the rectangles [io.archinaut.tenter.screen.Layout] splits from the frame's
 * area, and are clipped to the canvas this widget renders into.
 *
 * Each panel's viewport settles as it paints; [PanelSet.layout] is published once every panel has
 * painted. A failed frame keeps the previous layout and rolls nothing back, so discard its canvas;
 * the next frame resolves every panel again.
 */
public class Panels<K : PanelId, I>(
    private val inputs: I,
    private val areas: Map<K, Rect>,
) : StatefulWidget<PanelSet<K, I>> {

    override fun render(canvas: Canvas, state: PanelSet<K, I>) {
        state.render(canvas, inputs, areas)
    }
}
