// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/** A run of text in one [style]. */
public data class Span(
    public val text: String,
    public val style: Style = Style.DEFAULT,
)
