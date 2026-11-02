// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package tenterexample.colors

import io.archinaut.tenter.color.ColorSpace

/** The [scheme] the showcase draws with, and whether `--scheme` chose it or the terminal did. */
internal sealed interface SchemeChoice {
    val scheme: ShowcaseScheme

    /** The scheme as the status bar names it. */
    val description: String

    /** The scheme matching what the terminal shows. */
    data class Automatic(override val scheme: ShowcaseScheme) : SchemeChoice {
        override val description: String get() = "${scheme.label} (auto)"
    }

    /** The scheme `--scheme` named. */
    data class Explicit(override val scheme: ShowcaseScheme) : SchemeChoice {
        override val description: String get() = "${scheme.label} (--scheme)"
    }

    companion object {
        /**
         * The scheme [args] ask for with `--scheme=<name>`, else the one matching [terminal], with a
         * warning when the requested one is richer than the terminal; or why the name is unknown.
         */
        fun from(args: Array<String>, terminal: ColorSpace?): Result {
            val requested = args.lastOrNull { it.startsWith(OPTION) }?.removePrefix(OPTION)
                ?: return Result.Chosen(Automatic(ShowcaseScheme.forTerminal(terminal)), warning = null)
            val scheme = ShowcaseScheme.named(requested)
                ?: return Result.Unknown(
                    "Unknown color scheme '$requested'; choose one of: ${ShowcaseScheme.entries.joinToString { it.label }}",
                )
            val warning = if (scheme.exceeds(terminal)) {
                "This terminal shows ${terminal?.name?.lowercase() ?: "no color"}; '${scheme.label}' colors may look wrong."
            } else {
                null
            }
            return Result.Chosen(Explicit(scheme), warning)
        }

        private const val OPTION: String = "--scheme="
    }

    /** What [from] made of the arguments. */
    sealed interface Result {
        data class Chosen(
            val choice: SchemeChoice,
            val warning: String?,
        ) : Result

        data class Unknown(val message: String) : Result
    }
}
