// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample

/** What [runHeadlessSmoke] exercised: every page at every size, and the marquee moving. */
public data class SmokeResult(
    public val pageRenders: Int,
    public val marqueeMoved: Boolean,
)
