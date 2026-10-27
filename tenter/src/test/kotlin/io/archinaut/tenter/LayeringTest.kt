// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter

import com.lemonappdev.konsist.api.Konsist
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Enforces the internal layering `docs/architecture.md` describes for this module —
 * `text/input -> color -> screen -> widget -> animation/terminal`, with terminal
 * consuming input, color, screen, and widget for frame drawing and scoped lifecycle, never
 * the reverse — as an allowed-dependency matrix
 * between `tenter`'s direct child packages. [ArchitectureTest] enforces the module's *external*
 * seam (no `battletech.*` import, nothing outside an approved third-party allowlist); this
 * enforces the seams *between* its own packages, which nothing previously checked — a
 * a `io.archinaut.tenter.screen` file importing `io.archinaut.tenter.widget` passed every existing test.
 */
class LayeringTest {

    private val mainFiles = Konsist.scopeFromProject()
        .files
        .filter { it.path.contains("/tenter/") && !it.path.contains("/test/") }

    private val allowed: Map<String, Set<String>> = mapOf(
        "text" to emptySet(),
        "input" to emptySet(),
        "color" to emptySet(),
        "screen" to setOf("color", "text"),
        "animation" to setOf("screen", "text", "widget"),
        "terminal" to setOf("input", "color", "screen", "widget"),
        "widget" to setOf("input", "color", "screen", "text"),
    )

    /** The direct child of `tenter` [packageName] belongs to, e.g. `io.archinaut.tenter.widget` -> `"widget"`. */
    private fun topPackage(packageName: String): String =
        packageName.removePrefix("io.archinaut.tenter.").substringBefore('.')

    @Test
    fun `every package in the layering matrix has at least one file`() {
        allowed.keys.forEach { pkg ->
            val files = mainFiles.filter { it.packagee?.name?.startsWith("io.archinaut.tenter.$pkg") == true }
            assertTrue(
                files.isNotEmpty(),
                "no files found under io.archinaut.tenter.$pkg — a rename would silently disable its matrix row",
            )
        }
    }

    @Test
    fun `every package only imports from its declared layer`() {
        mainFiles.forEach { file ->
            val filePackage = file.packagee?.name ?: return@forEach
            if (!filePackage.startsWith("io.archinaut.tenter.")) return@forEach // e.g. root `tenter` package files
            val pkg = topPackage(filePackage)
            val allowedTargets = allowed[pkg] ?: error("io.archinaut.tenter.$pkg has no entry in the layering matrix — add one")
            val violations = file.imports.filter { imp ->
                imp.name.startsWith("io.archinaut.tenter.") &&
                    topPackage(imp.name) != pkg &&
                    topPackage(imp.name) !in allowedTargets
            }
            assertTrue(
                violations.isEmpty(),
                "${file.name} (io.archinaut.tenter.$pkg) imports outside its declared layer: ${violations.map { it.name }}",
            )
        }
    }
}
