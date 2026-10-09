// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.RevealRect
import io.archinaut.tenter.screen.Style

internal class RecordingTextSink(override val width: Int) : TextSink {
    private val instructions = mutableListOf<ContentLayout.Instruction>()

    override fun write(column: Int, row: Int, text: String, style: Style) {
        instructions += ContentLayout.Instruction.Text(column, row, text, style)
    }

    override fun reveal(x: Int, y: Int, width: Int, height: Int) {
        instructions += ContentLayout.Instruction.Reveal(RevealRect(x, y, width, height))
    }

    override fun place(layout: ContentLayout, x: Int, y: Int) {
        instructions += ContentLayout.Instruction.Child(x, y, layout)
    }

    internal fun snapshot(): List<ContentLayout.Instruction> = instructions.toList()
}
