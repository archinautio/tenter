// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.colormath.Color as ColorValue
import com.github.ajalt.colormath.model.Ansi16 as ColormathAnsi16
import com.github.ajalt.colormath.model.Ansi256 as ColormathAnsi256
import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.color.TerminalColor
import io.archinaut.tenter.color.ColorScheme
import io.archinaut.tenter.screen.Style

/**
 * Renders [Style]s to cached open/close ANSI escape strings.
 *
 * Mordant styles text through [TextStyle.invoke], which runs a regex scan over the styled text —
 * fine for occasional one-off printing, but too costly to repeat for every run of every row of
 * every frame. Each distinct [Style] is styled through Mordant exactly once, at first use,
 * and the result is split into an open/close pair ([Tags]) that the terminal can then paste
 * around plain text with no further Mordant involvement.
 *
 * [colorScheme] resolves every [ColorRole] to a [TerminalColor] already authored in the color scheme's
 * color space — there is no downsampling here or anywhere else in this class. [ansiLevel] is
 * used for exactly one thing: [AnsiLevel.NONE] suppresses tags entirely. Do not grow it back
 * into a conversion knob — that job belongs to which [ColorScheme] the caller chose.
 */
internal class StyleTagCache(private val colorScheme: ColorScheme, private val ansiLevel: AnsiLevel) {

    /** Cached open/close ANSI escape strings for one [Style]; either may be empty. */
    internal class Tags(internal val open: String, internal val close: String)

    // Per-role fg/bg color caches, lazily populated since a host application's ColorRole is an
    // open set (unlike the old closed Color enum, there is no fixed "every role" list to seed
    // these with eagerly). Separate maps because StandardRole.DEFAULT resolves to different foreground
    // and background values — a single cache keyed by role could not represent that.
    private val foregroundCache: HashMap<ColorRole, ColorValue> = HashMap()
    private val backgroundCache: HashMap<ColorRole, ColorValue> = HashMap()

    // Tags cache, keyed by the full Style. Lazily populated on first use for each distinct
    // Style — each entry pays Mordant's one-off styling cost exactly once.
    private val tagsCache: HashMap<Style, Tags> = HashMap()

    /** The open/close tag pair for [style], or `null` if it renders no ANSI codes at all. */
    internal fun tagsFor(style: Style): Tags? {
        if (ansiLevel == AnsiLevel.NONE) return null

        tagsCache[style]?.let { return it }

        val rendered = buildStyle(style)(SENTINEL)
        val split = rendered.indexOf(SENTINEL)
        val tags = if (split < 0) {
            Tags("", "")
        } else {
            Tags(rendered.substring(0, split), rendered.substring(split + SENTINEL.length))
        }
        tagsCache[style] = tags
        return tags
    }

    /** A channel no write set renders as [StandardRole.DEFAULT], or with the attribute off. */
    private fun buildStyle(style: Style): TextStyle {
        val fg = style.fg ?: StandardRole.DEFAULT
        val bg = style.bg ?: StandardRole.DEFAULT
        return TextStyle(
            color = foregroundCache.getOrPut(fg) { resolve(fg, background = false).toColormathColor() },
            bgColor = backgroundCache.getOrPut(bg) { resolve(bg, background = true).toColormathColor() },
            bold = style.bold ?: false,
            dim = style.dim ?: false,
            italic = style.italic ?: false,
            underline = style.underline ?: false,
            inverse = style.inverse ?: false,
            strikethrough = style.strikethrough ?: false,
        )
    }

    private fun resolve(role: ColorRole, background: Boolean): TerminalColor = when (role) {
        is TerminalColor -> role
        else -> if (background) colorScheme.background(role) else colorScheme.foreground(role)
    }

    /** [TerminalColor] -> its colormath equivalent, matching the color space it was authored in. */
    private fun TerminalColor.toColormathColor(): ColorValue = when (this) {
        is TerminalColor.TrueColor -> RGB.from255(red, green, blue)
        is TerminalColor.Ansi256 -> ColormathAnsi256(index)
        is TerminalColor.Ansi16 -> ColormathAnsi16(code)
    }

    private companion object {
        // A literal space can't appear inside an SGR/OSC escape sequence, so styling a
        // single-space sentinel safely marks the boundary between Mordant's open and close tags.
        private const val SENTINEL = " "
    }
}
