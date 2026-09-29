// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.panel

/**
 * How much room a panel is taking right now. Ordered smallest-to-largest, which IS the cycling
 * order [Panel.cycleState] walks — declaration order here is behaviour, not decoration.
 */
public enum class PanelState { MINIMIZED, NORMAL, MAXIMIZED }
