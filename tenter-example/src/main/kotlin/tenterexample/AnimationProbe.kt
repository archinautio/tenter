// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import io.archinaut.tenter.widget.Widget

internal data class AnimationProbe(
    val firstFrame: Widget,
    val secondFrame: Widget,
    val completedFrameCount: Int,
)
