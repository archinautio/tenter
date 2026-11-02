// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.color

/**
 * A color role: a semantic one resolves through a [ColorScheme], so a theme can change it, and a
 * [TerminalColor] resolves to itself. [StandardRole] is the set Tenter's widgets use; applications
 * add their own.
 */
public interface ColorRole
