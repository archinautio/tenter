// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.negative

import io.archinaut.tenter.screen.Constraint
import io.archinaut.tenter.widget.Flow
import io.archinaut.tenter.widget.MeasurableWidget
import io.archinaut.tenter.widget.Widget

// Deliberate negative fixture: this directory is excluded from ordinary source sets.
public fun rawFlow(raw: Widget): MeasurableWidget = Flow(listOf(Constraint.Length(10) to raw))
