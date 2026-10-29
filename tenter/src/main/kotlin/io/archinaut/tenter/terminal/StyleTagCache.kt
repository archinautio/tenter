// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import com.github.ajalt.colormath.model.RGB
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
 * The [StyleTags] of a terminal that shows color. Mordant's [TextStyle.invoke] scans text with a
 * regex, too costly for every run of every frame, so each distinct [Style] goes through it once
 * and is cached as an open/close pair. Colors are used as [colorScheme] authored them, never
 * downsampled.
 */
internal class StyleTagCache(private val colorScheme: ColorScheme) : StyleTags {

    // Filled lazily, since roles are an open set; separate because DEFAULT differs between channels.
    private val foregroundCache: HashMap<ColorRole, ColorValue> = HashMap()
    private val backgroundCache: HashMap<ColorRole, ColorValue> = HashMap()

    private val tagsCache: HashMap<Style, Tags> = HashMap()

    override fun tagsFor(style: Style): Tags {
        tagsCache[style]?.let { return it }

        val rendered = buildStyle(style)(SENTINEL)
        val split = rendered.indexOf(SENTINEL)
        val tags = if (split < 0) {
            Tags.NONE
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
