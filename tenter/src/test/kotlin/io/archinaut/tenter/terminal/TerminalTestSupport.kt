// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas

/** Draws a frame whose cells are a copy of [buffer], resizing this terminal to match it first. */
internal fun Terminal.renderBuffer(buffer: Buffer): CompletedFrame {
    resize(buffer.width, buffer.height)
    return draw { frame ->
        Canvas.of(frame.buffer).blit(Canvas.of(buffer), 0, 0, 0, 0, buffer.width, buffer.height)
    }
}
