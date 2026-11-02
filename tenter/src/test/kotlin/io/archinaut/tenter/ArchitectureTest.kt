// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter

import com.lemonappdev.konsist.api.Konsist
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Enforces `tenter`'s external dependency boundary: it depends on nothing beyond
 * Kotlin/kotlinx/Mordant.
 */
class ArchitectureTest {

    private val mainFiles = Konsist.scopeFromProduction("tenter").files

    private val allowedImportPrefixes = listOf(
        "io.archinaut.tenter",
        "kotlin",
        "kotlinx",
        "java",
        "com.github.ajalt",
    )

    @Test
    fun `tenter only imports from an approved set of packages`() {
        mainFiles.forEach { file ->
            val violations = file.imports.filter { imp -> allowedImportPrefixes.none { imp.name.startsWith(it) } }
            assertTrue(
                violations.isEmpty(),
                "${file.name} imports outside the approved set: ${violations.map { it.name }}",
            )
        }
    }

    @Test
    fun `managed panel mutations stay behind the panel set seam`() {
        val panel = Konsist.scopeFromProduction("tenter")
            .classes(includeNested = false, includeLocal = false)
            .single { it.name == "Panel" }
        val managedMutations = panel.functions(includeNested = false, includeLocal = false)
            .filter { it.name in setOf("cycleState", "demoteFromMaximized", "scrollBy", "requestRecenter", "render") }

        assertTrue(
            managedMutations.isNotEmpty() && managedMutations.all { it.hasInternalModifier },
            "Panel runtime mutations must be internal; PanelSet is the public mutation seam",
        )
    }
}
