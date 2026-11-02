// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.pages

import com.github.ajalt.mordant.input.KeyboardEvent
import io.archinaut.tenter.input.KeyLayer
import io.archinaut.tenter.input.KeyMap
import io.archinaut.tenter.input.KeySection

/** The key map of a page with a single [layer], always active. */
internal class PageKeys<A : Any>(layer: KeyLayer<A>) {
    private val map: KeyMap<Unit, A> = KeyMap(mapOf(Unit to layer))

    internal val hints: List<KeySection> = listOf(map.hints(Unit))

    internal fun resolve(event: KeyboardEvent): A? = map.resolve(ACTIVE, event)

    private companion object {
        private val ACTIVE: List<Unit> = listOf(Unit)
    }
}
