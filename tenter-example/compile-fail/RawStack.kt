// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.negative

import io.archinaut.tenter.view.Stack
import io.archinaut.tenter.view.View

// Deliberate negative fixture: this directory is excluded from ordinary source sets.
public fun rawStack(raw: View): Stack = Stack(listOf(raw))
