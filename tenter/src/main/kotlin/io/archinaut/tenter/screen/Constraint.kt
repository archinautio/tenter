// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.screen

/**
 * How much of a [Layout]'s main axis one segment asks for, in the manner of Ratatui's
 * `Constraint`. See [Layout.split] for how competing constraints are resolved.
 */
public sealed interface Constraint {

    /** Exactly [length] cells. */
    public data class Length(public val length: Int) : Constraint {
        init {
            require(length >= 0) { "length must not be negative: $length" }
        }
    }

    /** [percent] percent of the available cells, rounded down. */
    public data class Percentage(public val percent: Int) : Constraint {
        init {
            require(percent in 0..100) { "percentage must be in 0..100: $percent" }
        }
    }

    /** [numerator] / [denominator] of the available cells, rounded down. */
    public data class Ratio(public val numerator: Int, public val denominator: Int) : Constraint {
        init {
            require(denominator > 0) { "ratio denominator must be positive: $denominator" }
            require(numerator in 0..denominator) { "ratio numerator must be in 0..$denominator: $numerator" }
        }
    }

    /** At least [min] cells; grows into leftover space when the layout has no [Fill] segment. */
    public data class Min(public val min: Int) : Constraint {
        init {
            require(min >= 0) { "min must not be negative: $min" }
        }
    }

    /** At most [max] cells; asks for [max] and shrinks before any [Min] when space is short. */
    public data class Max(public val max: Int) : Constraint {
        init {
            require(max >= 0) { "max must not be negative: $max" }
        }
    }

    /** A share of the leftover space proportional to [weight] among all fill segments. */
    public data class Fill(public val weight: Int = 1) : Constraint {
        init {
            require(weight >= 0) { "fill weight must not be negative: $weight" }
        }
    }
}
