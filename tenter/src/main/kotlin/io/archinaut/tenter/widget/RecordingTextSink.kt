// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Rect
import io.archinaut.tenter.screen.Style

/** Records what a [TextCursor] writes as the instructions of a [ContentLayout]. */
internal class RecordingTextSink(val width: Int) {
    private val instructions = mutableListOf<ContentLayout.Instruction>()

    fun write(column: Int, row: Int, text: String, style: Style) {
        instructions += ContentLayout.Instruction.Text(column, row, text, style)
    }

    fun reveal(rect: Rect) {
        instructions += ContentLayout.Instruction.Reveal(rect)
    }

    fun place(layout: ContentLayout, x: Int, y: Int) {
        instructions += ContentLayout.Instruction.Child(x, y, layout)
    }

    fun snapshot(): List<ContentLayout.Instruction> = instructions.toList()
}
