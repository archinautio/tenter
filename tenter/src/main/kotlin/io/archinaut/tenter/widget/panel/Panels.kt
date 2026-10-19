// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.widget.StatefulWidget

/**
 * Draws the [visible] panels of a [PanelSet] into the whole canvas, building each panel's
 * content from [inputs], in the manner of a Ratatui `StatefulWidget` over its state.
 * Read the completed frame's geometry from [PanelSet.layout] afterwards.
 *
 * Managed offsets and hit geometry publish together only after every slot paints successfully.
 * A failed frame retains queued scrolling intent; application callbacks and canvas writes are
 * not rolled back, so discard the failed frame's canvas.
 */
public class Panels<K : PanelId, I>(
    private val inputs: I,
    private val visible: Set<K>,
) : StatefulWidget<PanelSet<K, I>> {

    override fun render(canvas: Canvas, state: PanelSet<K, I>) {
        state.render(canvas, inputs, visible)
    }
}
