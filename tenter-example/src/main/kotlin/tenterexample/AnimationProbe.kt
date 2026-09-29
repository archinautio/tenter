// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

import io.archinaut.tenter.view.View

internal data class AnimationProbe(
    val firstFrame: View,
    val secondFrame: View,
    val completedFrameCount: Int,
)
