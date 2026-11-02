// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget

import io.archinaut.tenter.screen.Point
import io.archinaut.tenter.screen.Size

internal data class ScrollTargets(val x: ScrollTarget, val y: ScrollTarget) {
    fun offset(viewport: Size, maxOffset: Point): Point =
        Point(x.offset(viewport.width, maxOffset.x), y.offset(viewport.height, maxOffset.y))

    companion object {
        fun keep(offset: Point): ScrollTargets = ScrollTargets(ScrollTarget.Keep(offset.x), ScrollTarget.Keep(offset.y))
    }
}
