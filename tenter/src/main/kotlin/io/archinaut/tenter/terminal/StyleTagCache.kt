// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.colormath.Color as ColorValue
import com.github.ajalt.colormath.model.Ansi16 as ColormathAnsi16
import com.github.ajalt.colormath.model.Ansi256 as ColormathAnsi256
import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.color.TerminalColor
import io.archinaut.tenter.color.ColorScheme
import io.archinaut.tenter.screen.Cell

/**
 * The [StyleTags] of a terminal that shows color. Mordant's [TextStyle.invoke] scans text with a
 * regex, too costly for every run of every frame, so each distinct look of a [Cell] goes through it
 * once and is cached as an open/close pair. Colors are used as [colorScheme] authored them, never
 * downsampled.
 */
internal class StyleTagCache(private val colorScheme: ColorScheme) : StyleTags {

    // Filled lazily, since roles are an open set; separate because DEFAULT differs between channels.
    private val foregroundCache: HashMap<ColorRole, ColorValue> = HashMap()
    private val backgroundCache: HashMap<ColorRole, ColorValue> = HashMap()

    // Keyed by foreground, then background, then the attribute bits, so a lookup builds no key object.
    private val tagsCache: HashMap<ColorRole, HashMap<ColorRole, Array<Tags?>>> = HashMap()

    override fun tagsFor(cell: Cell): Tags {
        val byAttributes = tagsCache.getOrPut(cell.fg) { HashMap() }.getOrPut(cell.bg) { arrayOfNulls(ATTRIBUTE_COMBINATIONS) }
        byAttributes[cell.attributes]?.let { return it }

        val rendered = textStyle(cell)(SENTINEL)
        val split = rendered.indexOf(SENTINEL)
        val tags = if (split < 0) {
            Tags.NONE
        } else {
            Tags(rendered.substring(0, split), rendered.substring(split + SENTINEL.length))
        }
        byAttributes[cell.attributes] = tags
        return tags
    }

    private fun textStyle(cell: Cell): TextStyle = TextStyle(
        color = foregroundCache.getOrPut(cell.fg) { resolve(cell.fg, background = false).toColormathColor() },
        bgColor = backgroundCache.getOrPut(cell.bg) { resolve(cell.bg, background = true).toColormathColor() },
        bold = cell.bold,
        dim = cell.dim,
        italic = cell.italic,
        underline = cell.underline,
        inverse = cell.inverse,
        strikethrough = cell.strikethrough,
    )

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
        /** Every combination of a cell's six attribute bits. */
        private const val ATTRIBUTE_COMBINATIONS = 64

        // A literal space can't appear inside an SGR/OSC escape sequence, so styling a
        // single-space sentinel safely marks the boundary between Mordant's open and close tags.
        private const val SENTINEL = " "
    }
}
