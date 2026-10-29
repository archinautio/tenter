// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.widget.panel

/**
 * Which content builder a panel uses; it implies no size. Declared smallest first, which is the
 * order [PanelSet.cycleFocusedMode] steps through.
 */
public enum class PanelMode { MINIMIZED, NORMAL, MAXIMIZED }
