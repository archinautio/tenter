// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.negative

import io.archinaut.tenter.widget.Column
import io.archinaut.tenter.widget.Widget

// Deliberate negative fixture: this directory is excluded from ordinary source sets.
public fun rawColumn(raw: Widget): Column = Column(10, raw)
