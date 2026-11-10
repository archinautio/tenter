// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class CellWidthTest {

    @Test
    fun `ascii printable chars are width 1`() {
        assertEquals(1, CellWidth.of('A'.code))
        assertEquals(1, CellWidth.of(' '.code))
        assertEquals(1, CellWidth.of('~'.code))
    }

    @Test
    fun `control chars are width 0`() {
        assertEquals(0, CellWidth.of(0x00))
        assertEquals(0, CellWidth.of(0x1F))
        assertEquals(0, CellWidth.of(0x7F))
    }

    @Test
    fun `box drawing chars are width 1`() {
        assertEquals(1, CellWidth.of('│'.code))
        assertEquals(1, CellWidth.of('─'.code))
        assertEquals(1, CellWidth.of('╭'.code))
    }

    @Test
    fun `nerd-font dice icons in supplementary PUA are width 1`() {
        // nf-md-dice_1..6 → U+F01CA..U+F01CF
        for (cp in 0xF01CA..0xF01CF) {
            assertEquals(1, CellWidth.of(cp), "codepoint U+%04X".format(cp))
        }
    }

    @Test
    fun `CJK ideographs are width 2`() {
        assertEquals(2, CellWidth.of(0x4E2D)) // 中
        assertEquals(2, CellWidth.of(0x65E5)) // 日
    }

    @Test
    fun `supplementary CJK planes are width 2 at their edges`() {
        assertEquals(1, CellWidth.of(0x1FFFD))
        assertEquals(2, CellWidth.of(0x20000)) // 𠀀
        assertEquals(2, CellWidth.of(0x2FFFD))
        assertEquals(2, CellWidth.of(0x30000)) // 𰀀
        assertEquals(2, CellWidth.of(0x3FFFD))
        assertEquals(1, CellWidth.of(0x40000))
    }

    @Test
    fun `emoji are width 2`() {
        assertEquals(2, CellWidth.of(0x1F600)) // 😀
    }

    @ParameterizedTest(name = "U+{0} is width 2")
    @ValueSource(
        strings = [
            "231A", // ⌚ watch
            "231B", // ⌛ hourglass
            "23F0", // ⏰ alarm clock
            "26A1", // ⚡ high voltage
            "26BD", // ⚽ soccer ball
            "2705", // ✅ check mark button
            "274C", // ❌ cross mark
            "2B50", // ⭐ star
            "1F004", // 🀄 mahjong red dragon
            "1F0CF", // 🃏 joker
            "1F18E", // 🆎 AB button
            "1F201", // 🈁 Japanese "here" button
            "1F680", // 🚀 rocket
            "1F6D1", // 🛑 stop sign
            "1F6F8", // 🛸 flying saucer
            "1F7E2", // 🟢 green circle
            "1FA90", // 🪐 ringed planet
            "1FAE0", // 🫠 melting face
        ],
    )
    fun `emoji-presentation emoji are width 2`(hex: String) {
        val codePoint = hex.toInt(16)
        assertEquals(2, CellWidth.of(codePoint))
        assertEquals(2, CellWidth.of(String(Character.toChars(codePoint))))
    }

    @ParameterizedTest(name = "U+{0} is width 1")
    @ValueSource(
        strings = [
            "2600", // ☀ sun
            "2603", // ☃ snowman
            "26A0", // ⚠ warning
            "1F000", // 🀀 mahjong east wind
            "1F0A1", // 🂡 ace of spades
            "1F6E0", // 🛠 hammer and wrench
            "1F6F3", // 🛳 passenger ship
        ],
    )
    fun `text-presentation neighbors of emoji stay width 1`(hex: String) {
        assertEquals(1, CellWidth.of(hex.toInt(16)))
    }

    @Test
    fun `emoji widths add up in a string`() {
        assertEquals(8, CellWidth.of("🚀 go ✅")) // 🚀 2, " go " 4, ✅ 2
    }

    @Test
    fun `string width sums codepoints, not UTF-16 chars`() {
        val dice = String(Character.toChars(0xF01CA)) + String(Character.toChars(0xF01CB))
        assertEquals(4, dice.length)               // sanity: 2 surrogate pairs = 4 UTF-16 chars
        assertEquals(2, CellWidth.of(dice))         // but only 2 visual cells
    }

    @Test
    fun `mixed ASCII and nerd-font string`() {
        val text = "P1 " + String(Character.toChars(0xF01CA)) + String(Character.toChars(0xF01CB)) + " 9"
        // "P1 " = 3, two dice = 2, " 9" = 2 → 7
        assertEquals(7, CellWidth.of(text))
    }

    @Test
    fun `grapheme clusters are measured atomically`() {
        assertEquals(2, CellWidth.of("👩‍💻"))
        assertEquals(1, CellWidth.of("🇪🇸"))
        assertEquals(1, CellWidth.of("1️⃣"))
        assertEquals(1, CellWidth.of("❤️"))
        assertEquals(1, CellWidth.of("e\u0301"))
    }

    @Test
    fun `standalone combining marks and terminal controls have no display width`() {
        assertEquals(0, CellWidth.of("\u0301\u20DD"))
        assertEquals(2, CellWidth.of("a\t\u001Bb"))
    }

    @Test
    fun `isolated surrogates use replacement character width`() {
        assertEquals(1, CellWidth.of("\uD800"))
        assertEquals(1, CellWidth.of(0xD800))
    }
}
