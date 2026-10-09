// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Style

internal interface TextSink {
    val width: Int

    fun write(column: Int, row: Int, text: String, style: Style)

    fun reveal(x: Int, y: Int, width: Int, height: Int)

    fun place(layout: ContentLayout, x: Int, y: Int)
}
