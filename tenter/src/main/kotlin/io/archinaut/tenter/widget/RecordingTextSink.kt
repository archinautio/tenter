// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

/** Records what a [RowWriter] writes as the instructions of a [ContentLayout]. */
internal class RecordingTextSink(val width: Int) {
    private val instructions = mutableListOf<ContentLayout.Instruction>()

    /** How far the recorded content reaches: [width], or further when a placed child is wider. */
    var occupiedWidth: Int = width
        private set

    fun write(column: Int, row: Int, text: String, style: Style) {
        instructions += ContentLayout.Instruction.Text(column, row, text, style)
    }

    fun reveal(rect: Rect) {
        instructions += ContentLayout.Instruction.Reveal(rect)
    }

    fun place(layout: ContentLayout, x: Int, y: Int) {
        instructions += ContentLayout.Instruction.Child(x, y, layout)
        occupiedWidth = maxOf(occupiedWidth, checkedAdd(x, layout.width, "placed child width"))
    }

    fun snapshot(): List<ContentLayout.Instruction> = instructions.toList()
}
