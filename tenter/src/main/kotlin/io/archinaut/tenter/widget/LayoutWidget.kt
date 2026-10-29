// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

/** A measurable widget that places prepared children at explicit coordinates instead of writing rows. */
internal interface LayoutWidget : MeasurableWidget {
    /** Lays out this widget for [availableWidth], which [prepare] has checked is not negative. */
    fun layout(availableWidth: Int): ContentLayout

    override fun write(writer: RowWriter) {
        writer.draw(this)
    }
}
