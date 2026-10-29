// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.terminal

import io.archinaut.tenter.screen.Buffer
import io.archinaut.tenter.screen.Canvas
import io.archinaut.tenter.screen.Cell
import io.archinaut.tenter.screen.Style

import io.archinaut.tenter.color.StandardRole
import io.archinaut.tenter.color.ColorRole
import io.archinaut.tenter.color.TerminalColor
import io.archinaut.tenter.color.ColorScheme
import io.archinaut.tenter.color.ColorSpace

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.archinaut.tenter.color.TerminalColor.TrueColor

/** A color scheme resolving every [StandardRole], so tests can draw with any [Style]. */
private fun rgb(r: Int, g: Int, b: Int) = TerminalColor.TrueColor(r, g, b)

private object FixtureColorScheme : ColorScheme {
    override val defaultBackground: TerminalColor = rgb(16, 20, 24)

    override fun foreground(role: ColorRole): TerminalColor = when (role) {
        is StandardRole -> when (role) {
            StandardRole.DEFAULT -> rgb(221, 226, 229)
            StandardRole.TEXT_PRIMARY -> rgb(241, 243, 245)
            StandardRole.TEXT_MUTED -> rgb(166, 173, 180)
            StandardRole.TEXT_SUBTLE -> rgb(76, 80, 84)
            StandardRole.ACCENT -> rgb(255, 209, 102)
            StandardRole.INFO -> rgb(119, 212, 232)
            StandardRole.SUCCESS -> rgb(139, 209, 124)
            StandardRole.WARNING -> rgb(243, 211, 106)
            StandardRole.DANGER -> rgb(255, 153, 153)
            StandardRole.DISABLED -> rgb(137, 145, 152)
            StandardRole.PANEL_BORDER -> rgb(114, 191, 114)
            StandardRole.PANEL_BORDER_FOCUSED -> rgb(114, 191, 114)
        }
        else -> error("FixtureColorScheme only resolves StandardRole, got: $role")
    }
}

private object AltFixtureColorScheme : ColorScheme {
    override val defaultBackground: TerminalColor = rgb(248, 245, 238)

    override fun foreground(role: ColorRole): TerminalColor = when (role) {
        is StandardRole -> rgb(32, 36, 40)
        else -> error("AltFixtureColorScheme only resolves StandardRole, got: $role")
    }
}

internal class TerminalTest {

    private val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
    private val backend = MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = recorder)
    private val screen = Terminal(backend).openScreen(FixtureColorScheme).also { recorder.clearOutput() }

    @Test
    fun `fixed colors render through a custom color scheme in both channels`() {
        val customColorScheme = object : ColorScheme {
            override val defaultBackground: TerminalColor = TrueColor(0, 0, 0)

            override fun foreground(role: ColorRole): TerminalColor = when (role) {
                is StandardRole -> TrueColor(255, 255, 255)
                else -> error("semantic role unexpectedly resolved: $role")
            }

            override fun background(role: ColorRole): TerminalColor = when (role) {
                StandardRole.DEFAULT -> defaultBackground
                is StandardRole -> TrueColor(0, 0, 0)
                else -> error("semantic role unexpectedly resolved: $role")
            }
        }
        val recorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val customBackend = MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = recorder)
        val fixed = TrueColor(255, 0, 0)
        val buffer = Buffer(1, 1)
        buffer.set(0, 0, Cell("X", Style(fg = fixed, bg = fixed)))

        Terminal(customBackend).openScreen(customColorScheme).also { recorder.clearOutput() }.renderBuffer(buffer)

        assertTrue(recorder.output().contains("38;2;255;0;0"))
        assertTrue(recorder.output().contains("48;2;255;0;0"))
    }

    @Test
    fun `output starts with cursor home sequence`() {
        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "abc")

        screen.renderBuffer(buffer)

        // Positions are 1-indexed: (0, 0) is ESC[1;1H.
        assertTrue(
            recorder.output().startsWith("[1;1H"),
            "Expected output to start with ESC[1;1H but was: ${recorder.output().take(20).repr()}"
        )
    }

    @Test
    fun `default cell emits the color scheme's explicit foreground and background truecolor sequences`() {
        // StandardRole.DEFAULT resolves to a real color, so even a default cell is styled.
        val buffer = Buffer(5, 1)
        Canvas.of(buffer).setString(0, 0, "hello")

        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains("hello"), "Expected 'hello' in output: ${out.repr()}")
        assertTrue(out.contains("38;2;221;226;229"), "Expected the fixture's default foreground: ${out.repr()}")
        assertTrue(out.contains("48;2;16;20;24"), "Expected the fixture's default background: ${out.repr()}")
    }

    @Test
    fun `a channel no write set renders as the color scheme default`() {
        val buffer = Buffer(1, 1)
        Canvas.of(buffer).setString(0, 0, "X", Style(fg = StandardRole.DANGER))

        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains("38;2;255;153;153"), "Expected DANGER's foreground: ${out.repr()}")
        assertTrue(out.contains("48;2;16;20;24"), "Expected the fixture's default background: ${out.repr()}")
    }

    @Test
    fun `default cell emits a different color scheme's sequences when constructed with it`() {
        val altRecorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val altBackend = MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = altRecorder)
        val altScreen = Terminal(altBackend).openScreen(AltFixtureColorScheme).also { altRecorder.clearOutput() }

        val buffer = Buffer(5, 1)
        Canvas.of(buffer).setString(0, 0, "hello")
        altScreen.renderBuffer(buffer)

        val out = altRecorder.output()
        assertTrue(out.contains("38;2;32;36;40"), "Expected AltFixtureColorScheme's foreground: ${out.repr()}")
        assertTrue(out.contains("48;2;248;245;238"), "Expected AltFixtureColorScheme's default background: ${out.repr()}")
    }

    @Test
    fun `colored run emits one style per run not one per cell`() {
        val buffer = Buffer(4, 1)
        Canvas.of(buffer).setString(0, 0, "ABCD", Style(fg = StandardRole.DANGER))

        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains("ABCD"), "Expected 'ABCD' to appear contiguously in output: ${out.repr()}")

        // Every style sets both channels, so each run closes with one combined "39;49m".
        val resetSeq = "39;49m"
        val resetCount = out.countOccurrences(resetSeq)
        assertEquals(1, resetCount, "Expected exactly 1 fg+bg-reset sequence for a single run, got $resetCount in: ${out.repr()}")
    }

    @Test
    fun `per-cell styling would produce four resets but run-length produces one`() {
        val buffer = Buffer(4, 1)
        for (x in 0 until 4) {
            buffer.set(x, 0, Cell("X", Style(fg = StandardRole.DANGER)))
        }

        screen.renderBuffer(buffer)

        val out = recorder.output()
        val resetSeq = "39;49m"
        val resetCount = out.countOccurrences(resetSeq)
        assertEquals(1, resetCount, "4 same-style cells should produce exactly 1 reset, got $resetCount")
    }

    @Test
    fun `strikethrough style emits strikethrough SGR once per run`() {
        // Mordant folds color and strikethrough into one compound SGR open.
        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "XYZ", Style(fg = StandardRole.DANGER, strikethrough = true))

        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains("XYZ"), "Expected 'XYZ' to appear contiguously in output: ${out.repr()}")

        // ";9m" turns strikethrough on with the color; ";29m" turns it off with the reset.
        val strikeOnCount = out.countOccurrences(";9m")
        val strikeOffCount = out.countOccurrences(";29m")
        assertEquals(
            1,
            strikeOnCount,
            "Expected exactly 1 strikethrough-on sequence for a single run, got $strikeOnCount in: ${out.repr()}",
        )
        assertEquals(
            1,
            strikeOffCount,
            "Expected exactly 1 strikethrough-off sequence for a single run, got $strikeOffCount in: ${out.repr()}",
        )
    }

    @Test
    fun `text attributes render through their SGR codes`() {
        val buffer = Buffer(5, 1)
        buffer.set(0, 0, Cell("B", Style(bold = true)))
        buffer.set(1, 0, Cell("D", Style(dim = true)))
        buffer.set(2, 0, Cell("I", Style(italic = true)))
        buffer.set(3, 0, Cell("U", Style(underline = true)))
        buffer.set(4, 0, Cell("V", Style(inverse = true)))

        screen.renderBuffer(buffer)

        val out = recorder.output()
        for ((code, glyph) in listOf(1 to "B", 2 to "D", 3 to "I", 4 to "U", 7 to "V")) {
            val opening = Regex("((?:\u001B\\[[\\d;]*m)+)$glyph").find(out)?.groupValues?.get(1).orEmpty()
            assertTrue(code in sgrAttributes(opening), "Expected SGR $code right before '$glyph': ${out.repr()}")
        }
    }

    @Test
    fun `an attribute does not bleed into a following run that only changes color`() {
        // Only colors can skip a run's close; a change of attribute must still close bold.
        val buffer = Buffer(2, 1)
        buffer.set(0, 0, Cell("◆", Style(fg = StandardRole.DANGER, bold = true)))
        buffer.set(1, 0, Cell("◇", Style(fg = StandardRole.INFO)))

        screen.renderBuffer(buffer)

        val out = recorder.output()
        val between = out.substring(out.indexOf("◆") + 1, out.indexOf("◇"))
        assertTrue(22 in sgrAttributes(between), "Expected bold to close between runs: ${out.repr()}")
    }

    @Test
    fun `transitions between default and semantic roles do not leak either color channel`() {
        // A run that skips the previous close must still set both of its own colors.
        val buffer = Buffer(2, 1)
        buffer.set(0, 0, Cell("A", Style(fg = StandardRole.DANGER, bg = StandardRole.SUCCESS)))
        buffer.set(1, 0, Cell("B", Style.DEFAULT))

        screen.renderBuffer(buffer)

        val out = recorder.output()
        val betweenAAndB = out.substringAfter("A").substringBefore("B")
        assertTrue(
            betweenAAndB.contains("38;2;221;226;229"),
            "Expected the DEFAULT cell to explicitly set its own foreground, not inherit DANGER's: ${out.repr()}",
        )
        assertTrue(
            betweenAAndB.contains("48;2;16;20;24"),
            "Expected the DEFAULT cell to explicitly set its own background, not inherit SUCCESS's: ${out.repr()}",
        )
    }

    @Test
    fun `wide character written via setString appears in output`() {
        // A wide glyph's continuation cell contributes nothing to the output.
        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "中") // 中, width=2

        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains("中"), "Expected wide char '中' in output: ${out.repr()}")
        assertEquals(1, out.countOccurrences("中"), "Wide char should appear exactly once")
    }

    @Test
    fun `rows separated by carriage-return newline`() {
        val buffer = Buffer(2, 3)
        Canvas.of(buffer).setString(0, 0, "AB")
        Canvas.of(buffer).setString(0, 1, "CD")
        Canvas.of(buffer).setString(0, 2, "EF")

        screen.renderBuffer(buffer)

        val out = recorder.output()
        // Rows are joined with \r\n and each closes its own run, so a close tag sits before each separator.
        val firstBreak = out.indexOf("\r\n")
        val secondBreak = out.indexOf("\r\n", firstBreak + 1)
        assertTrue(firstBreak >= 0 && secondBreak >= 0, "Expected two \\r\\n row separators in: ${out.repr()}")
        assertEquals(2, out.countOccurrences("\r\n"), "Expected exactly two row separators (3 rows, no trailing one): ${out.repr()}")
        assertTrue(out.indexOf("AB") in 0..<firstBreak, "Expected 'AB' before the first row break: ${out.repr()}")
        assertTrue(out.indexOf("CD") in firstBreak..<secondBreak, "Expected 'CD' between the two row breaks: ${out.repr()}")
        assertTrue(out.indexOf("EF") > secondBreak, "Expected 'EF' after the second row break: ${out.repr()}")
    }

    // ---- dirty-cell diffing (draw() keeps the last-sent buffer and only sends what changed) ----

    @Test
    fun `second render of an identical buffer emits nothing`() {
        val first = Buffer(5, 2)
        Canvas.of(first).setString(0, 0, "hello")
        screen.renderBuffer(first)

        // A separate buffer with the same content: draw() diffs by value, not identity.
        val second = Buffer(5, 2)
        Canvas.of(second).setString(0, 0, "hello")
        recorder.clearOutput()
        screen.renderBuffer(second)

        assertEquals("", recorder.output(), "An unchanged frame must produce no output at all")
    }

    @Test
    fun `single changed cell emits only that cell, not the rest of the row`() {
        val first = Buffer(5, 1)
        Canvas.of(first).setString(0, 0, "hello")
        screen.renderBuffer(first)

        // Only column 2 differs ('l' -> 't').
        val second = Buffer(5, 1)
        Canvas.of(second).setString(0, 0, "hetlo")
        recorder.clearOutput()
        screen.renderBuffer(second)

        val out = recorder.output()
        assertTrue(out.contains("t"), "Expected the changed cell 't' in the diff output: ${out.repr()}")
        assertTrue(
            !out.contains("hello") && !out.contains("hetlo"),
            "Expected only the changed cell to be sent, not the whole row: ${out.repr()}",
        )
    }

    @Test
    fun `a changed wide character re-emits its lead glyph, not a lone continuation cell`() {
        val first = Buffer(4, 1)
        Canvas.of(first).setString(0, 0, "a中b")
        screen.renderBuffer(first)

        // Swap the wide glyph for a different one of the same width, at the same position.
        val second = Buffer(4, 1)
        Canvas.of(second).setString(0, 0, "a日b")
        recorder.clearOutput()
        screen.renderBuffer(second)

        val out = recorder.output()
        assertTrue(out.contains("日"), "Expected the new wide glyph to be resent whole: ${out.repr()}")
        assertTrue(!out.contains("中"), "Expected the old glyph to be gone: ${out.repr()}")
    }

    @Test
    fun `replacing a wide glyph with narrow text repaints its full old footprint`() {
        val first = Buffer(3, 1)
        Canvas.of(first).setString(0, 0, "中A")
        screen.renderBuffer(first)

        val second = Buffer(3, 1)
        Canvas.of(second).setString(0, 0, "XA")
        recorder.clearOutput()
        screen.renderBuffer(second)

        val out = recorder.output()
        assertTrue(out.contains("XA"), "Expected both columns of the old wide footprint to be repainted: ${out.repr()}")
        assertTrue(!out.contains("中"), "Expected the old wide glyph to be absent: ${out.repr()}")
    }

    @Test
    fun `replacing a wide continuation emits the replacement and clears its lead`() {
        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "中A")
        screen.renderBuffer(buffer)

        buffer.set(1, 0, Cell("X"))
        recorder.clearOutput()
        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains(" X"), "Expected the lead to be erased before the replacement: ${out.repr()}")
        assertTrue(!out.contains("中"), "Expected the old wide glyph to be absent: ${out.repr()}")
    }

    @Test
    fun `changing a wide glyph style repaints its complete terminal footprint`() {
        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "中", Style(fg = StandardRole.DANGER))
        screen.renderBuffer(buffer)

        Canvas.of(buffer).setString(0, 0, "中", Style(fg = StandardRole.SUCCESS))
        recorder.clearOutput()
        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains("中"), "Expected the styled wide glyph to be repainted: ${out.repr()}")
        assertTrue(out.contains("38;2;139;209;124"), "Expected the replacement style: ${out.repr()}")
    }

    @Test
    fun `mutating a submitted buffer is visible on the next render`() {
        val buffer = Buffer(1, 1)
        Canvas.of(buffer).setString(0, 0, "A")
        screen.renderBuffer(buffer)

        buffer.set(0, 0, Cell("B"))
        recorder.clearOutput()
        screen.renderBuffer(buffer)

        assertTrue(recorder.output().contains("B"), "Expected the reused buffer's new cell: ${recorder.output().repr()}")
    }

    @Test
    fun `a terminal size change forces a full repaint even though content is unchanged`() {
        val first = Buffer(3, 1)
        Canvas.of(first).setString(0, 0, "abc")
        screen.renderBuffer(first)

        // Same content, different dimensions.
        val resized = Buffer(4, 1)
        Canvas.of(resized).setString(0, 0, "abc")
        recorder.clearOutput()
        screen.renderBuffer(resized)

        assertTrue(
            recorder.output().startsWith("[1;1H"),
            "Expected a full repaint (cursor-home) after a size change, got: ${recorder.output().take(20).repr()}",
        )
    }

    @Test
    fun `shrinking the buffer clears stale content before repainting`() {
        val first = Buffer(4, 1)
        Canvas.of(first).setString(0, 0, "ABCD")
        screen.renderBuffer(first)

        val smaller = Buffer(1, 1)
        Canvas.of(smaller).setString(0, 0, "Z")
        recorder.clearOutput()
        screen.renderBuffer(smaller)

        assertTrue(recorder.output().contains("[2J"), "Expected stale terminal content to be erased: ${recorder.output().repr()}")
        assertTrue(recorder.output().contains("Z"), "Expected the smaller frame to be painted: ${recorder.output().repr()}")
    }

    // ---- entering a screen ----

    @Test
    fun `entering a screen paints the color scheme's default background before erasing, not the terminal's own`() {
        // Erasing before setting the default style would flash the terminal's own background.
        val altRecorder = TerminalRecorder(ansiLevel = AnsiLevel.TRUECOLOR)
        val altBackend = MordantTerminal(ansiLevel = AnsiLevel.TRUECOLOR, terminalInterface = altRecorder)

        Terminal(altBackend).openScreen(AltFixtureColorScheme)

        val out = altRecorder.output()
        val bgIndex = out.indexOf("48;2;248;245;238")
        val eraseIndex = out.indexOf("[2J")
        assertTrue(bgIndex >= 0, "Expected AltFixtureColorScheme's default background before clearing: ${out.repr()}")
        assertTrue(eraseIndex >= 0, "Expected an erase-display sequence: ${out.repr()}")
        assertTrue(bgIndex < eraseIndex, "Expected the background tag before the erase sequence: ${out.repr()}")
    }

    @Test
    fun `AnsiLevel NONE emits no SGR codes at all`() {
        val noneRecorder = TerminalRecorder(ansiLevel = AnsiLevel.NONE)
        val noneBackend = MordantTerminal(ansiLevel = AnsiLevel.NONE, terminalInterface = noneRecorder)
        val noneScreen = Terminal(noneBackend).openScreen(FixtureColorScheme).also { noneRecorder.clearOutput() }

        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "abc", Style(fg = StandardRole.DANGER, strikethrough = true))
        noneScreen.renderBuffer(buffer)

        val out = noneRecorder.output()
        assertTrue(out.contains("abc"), "Expected plain text content: ${out.repr()}")
        assertTrue(
            !Regex("\\[[\\d;]+m").containsMatchIn(out),
            "Expected no SGR codes at all at AnsiLevel.NONE: ${out.repr()}",
        )
    }

    @Test
    fun `a terminal reports the color space its backend shows, or none`() {
        fun colorSpaceAt(level: AnsiLevel): ColorSpace? =
            Terminal(MordantTerminal(ansiLevel = level, terminalInterface = TerminalRecorder(ansiLevel = level))).colorSpace

        assertEquals(ColorSpace.TRUECOLOR, colorSpaceAt(AnsiLevel.TRUECOLOR))
        assertEquals(ColorSpace.ANSI256, colorSpaceAt(AnsiLevel.ANSI256))
        assertEquals(ColorSpace.ANSI16, colorSpaceAt(AnsiLevel.ANSI16))
        assertEquals(null, colorSpaceAt(AnsiLevel.NONE))
    }

    // ---- helpers ----

    /** The attribute codes in [escapes]' SGR sequences, skipping extended `38`/`48` color arguments. */
    private fun sgrAttributes(escapes: String): Set<Int> = buildSet {
        for (match in Regex("\u001B\\[([\\d;]*)m").findAll(escapes)) {
            val params = match.groupValues[1].split(';').mapNotNull(String::toIntOrNull)
            var i = 0
            while (i < params.size) {
                when (params[i]) {
                    38, 48 -> i += if (params.getOrNull(i + 1) == 2) 5 else 3
                    else -> add(params[i]).also { i++ }
                }
            }
        }
    }

    private fun String.repr(): String = this.replace("\u001B", "ESC").replace("\r", "\\r").replace("\n", "\\n")

    private fun String.countOccurrences(sub: String): Int {
        if (sub.isEmpty()) return 0
        var count = 0
        var idx = 0
        while (true) {
            idx = this.indexOf(sub, idx)
            if (idx == -1) break
            count++
            idx += sub.length
        }
        return count
    }
}
