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

/**
 * A minimal, fully-authored [ColorScheme] for exercising [Terminal]'s own mechanics —
 * diffing, run coalescing, alt-screen switching — independent of any host application's actual
 * theme. Every [StandardRole] is resolved so [Terminal] can be exercised with an arbitrary
 * [Style], not just the handful of roles individual tests reference.
 */
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

        // setPosition(0, 0) emits ESC[1;1H (1-indexed row;col) — the literal ESC byte is part of
        // the prefix, not just the bracket text that follows it.
        assertTrue(
            recorder.output().startsWith("[1;1H"),
            "Expected output to start with ESC[1;1H but was: ${recorder.output().take(20).repr()}"
        )
    }

    @Test
    fun `default cell emits the color scheme's explicit foreground and background truecolor sequences`() {
        // StandardRole.DEFAULT resolves to a real color — there is no "unstyled" cell on an
        // ANSI-capable terminal.
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
        // 4 cells all with fg=DANGER — they should be wrapped in a single SGR open+close pair
        val buffer = Buffer(4, 1)
        Canvas.of(buffer).setString(0, 0, "ABCD", Style(fg = StandardRole.DANGER))

        screen.renderBuffer(buffer)

        val out = recorder.output()
        // The characters must appear contiguous — no SGR codes between them
        assertTrue(out.contains("ABCD"), "Expected 'ABCD' to appear contiguously in output: ${out.repr()}")

        // Count closing/reset sequences for fg: ESC[39m is always the fg-reset code, regardless
        // of the foreground's own color space (truecolor/256/16) — see AnsiCodes.fgColorReset.
        // StandardRole.DEFAULT now sets a real background too, so every style sets both channels and
        // the close tag is always the combined "39;49m" (fg+bg reset in one SGR), never a bare
        // "39m" — see Terminal.renderSpan's KDoc. Each run emits exactly one close tag;
        // with one run we expect exactly one.
        val resetSeq = "39;49m"
        val resetCount = out.countOccurrences(resetSeq)
        assertEquals(1, resetCount, "Expected exactly 1 fg+bg-reset sequence for a single run, got $resetCount in: ${out.repr()}")
    }

    @Test
    fun `per-cell styling would produce four resets but run-length produces one`() {
        // Contrast: 4 consecutive DANGER cells → 1 reset (run-length) vs 4 resets (per-cell, old).
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
        // 3 consecutive cells with fg=DANGER + strikethrough — Mordant folds both attributes into
        // one compound SGR open, and one run should emit exactly one.
        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "XYZ", Style(fg = StandardRole.DANGER, strikethrough = true))

        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains("XYZ"), "Expected 'XYZ' to appear contiguously in output: ${out.repr()}")

        // ";9m" is the strikethrough-on code compounded onto the fg-open sequence;
        // ";29m" is strikethrough-off compounded onto the fg-reset sequence.
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
        // The run-length skip-close optimization only holds for colors; a bold run followed by a
        // plain run of another color must still close bold.
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
        // Every open tag now sets BOTH channels explicitly (StandardRole.DEFAULT resolves to real
        // colors too), so the run-length skip-close optimization never needs to special-case
        // DEFAULT — see Terminal.renderSpan's KDoc. Regression guard: a cell that follows a
        // differently-colored one must still get its own explicit fg AND bg, not inherit either
        // channel from the previous run.
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
        // U+4E2D is a CJK wide character (width=2). setString stores it in cell 0 and a
        // follow-up Cell("") in cell 1.  The terminal should include the character char and
        // the empty follow-up char (which contributes nothing), resulting in "中" appearing
        // once in the output.
        val buffer = Buffer(3, 1)
        Canvas.of(buffer).setString(0, 0, "中") // 中, width=2

        screen.renderBuffer(buffer)

        val out = recorder.output()
        assertTrue(out.contains("中"), "Expected wide char '中' in output: ${out.repr()}")
        // Should appear exactly once
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
        // Rows joined with \r\n, last row has no trailing \r\n. Each row is its own styled run
        // now (StandardRole.DEFAULT sets real colors), so "AB" and "CD" are no longer literally
        // adjacent to the \r\n — a close tag sits between the text and the separator — so this
        // checks ordering and separator count rather than one contiguous substring.
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

        // A fresh, separately-allocated buffer with the same content — draw() diffs by value,
        // not by instance identity.
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
        // "a中b": a(width 1) + 中(width 2, CJK) + b(width 1) — setString stores 中's lead
        // glyph at column 1 and an empty Cell("") continuation at column 2.
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
        // Regression guard for a cross-fade flash: the default-style SGR tag must be emitted before
        // the erase-display sequence, so ED erases using the color scheme's background rather than
        // whatever the terminal itself defaults to.
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
