// SPDX-FileCopyrightText: 2026 Alejandro Pérez García
// SPDX-License-Identifier: Apache-2.0

package io.archinaut.tenter.text

public object CellWidth {

    public fun of(codePoint: Int): Int = when {
        codePoint in 0xD800..0xDFFF -> of(0xFFFD)
        codePoint < 0x20 -> 0
        codePoint in 0x7F..0x9F -> 0
        isZeroWidth(codePoint) -> 0
        isWide(codePoint) -> 2
        else -> 1
    }

    public fun of(text: String): Int = clustersWidth(text)

    /** Display width of the `[startIndex, endIndex)` code-unit slice of [text]. */
    public fun of(text: String, startIndex: Int, endIndex: Int): Int {
        require(startIndex in 0..text.length) { "startIndex must be within text" }
        require(endIndex in startIndex..text.length) { "endIndex must be within text after startIndex" }
        return clustersWidth(text, startIndex, endIndex)
    }

    /** Whether [text] is exactly one drawable grapheme occupying one terminal cell. */
    public fun isSingleCellGlyph(text: String): Boolean {
        val clusters = textClusters(text).toList()
        return clusters.size == 1 && clusters.single().width == 1 && clusters.single().drawableText.isNotEmpty()
    }

    private fun isZeroWidth(cp: Int): Boolean =
        when (Character.getType(cp).toByte()) {
            Character.NON_SPACING_MARK,
            Character.COMBINING_SPACING_MARK,
            Character.ENCLOSING_MARK,
            Character.FORMAT -> true

            else -> false
        }

    // Called for every code point painted, so a bit lookup replaces scanning the ranges.
    private fun isWide(cp: Int): Boolean = when {
        cp < BITSET_LIMIT -> WIDE_BITS[cp ushr 6] and (1L shl cp) != 0L
        else -> cp in 0x20000..0x2FFFD || cp in 0x30000..0x3FFFD
    }

    /** Code points below this are looked up in [WIDE_BITS]; the CJK planes above it are whole ranges. */
    private const val BITSET_LIMIT: Int = 0x20000

    /**
     * Inclusive `start, end` pairs, sorted and disjoint: East Asian wide and fullwidth text, and
     * emoji whose default presentation is emoji. Emoji that default to text presentation, such as
     * U+2600 or U+1F6E0, stay narrow. Nerd-Font PUA (BMP U+E000..U+F8FF, supplementary
     * U+F0000..U+FFFFD) is intentionally absent: most terminals render those glyphs in one cell.
     */
    private val WIDE_RANGES: IntArray = intArrayOf(
        0x1100, 0x115F,
        0x231A, 0x231B,
        0x23E9, 0x23EC,
        0x23F0, 0x23F0,
        0x23F3, 0x23F3,
        0x25FD, 0x25FE,
        0x2614, 0x2615,
        0x2648, 0x2653,
        0x267F, 0x267F,
        0x2693, 0x2693,
        0x26A1, 0x26A1,
        0x26AA, 0x26AB,
        0x26BD, 0x26BE,
        0x26C4, 0x26C5,
        0x26CE, 0x26CE,
        0x26D4, 0x26D4,
        0x26EA, 0x26EA,
        0x26F2, 0x26F3,
        0x26F5, 0x26F5,
        0x26FA, 0x26FA,
        0x26FD, 0x26FD,
        0x2705, 0x2705,
        0x270A, 0x270B,
        0x2728, 0x2728,
        0x274C, 0x274C,
        0x274E, 0x274E,
        0x2753, 0x2755,
        0x2757, 0x2757,
        0x2795, 0x2797,
        0x27B0, 0x27B0,
        0x27BF, 0x27BF,
        0x2B1B, 0x2B1C,
        0x2B50, 0x2B50,
        0x2B55, 0x2B55,
        0x2E80, 0x303E,
        0x3041, 0x33FF,
        0x3400, 0x4DBF,
        0x4E00, 0x9FFF,
        0xA000, 0xA4CF,
        0xAC00, 0xD7A3,
        0xF900, 0xFAFF,
        0xFE30, 0xFE4F,
        0xFF00, 0xFF60,
        0xFFE0, 0xFFE6,
        0x1F004, 0x1F004,
        0x1F0CF, 0x1F0CF,
        0x1F18E, 0x1F18E,
        0x1F191, 0x1F19A,
        0x1F200, 0x1F202,
        0x1F210, 0x1F23B,
        0x1F240, 0x1F248,
        0x1F250, 0x1F251,
        0x1F260, 0x1F265,
        0x1F300, 0x1F64F,
        0x1F680, 0x1F6C5,
        0x1F6CC, 0x1F6CC,
        0x1F6D0, 0x1F6D2,
        0x1F6D5, 0x1F6D7,
        0x1F6DC, 0x1F6DF,
        0x1F6EB, 0x1F6EC,
        0x1F6F4, 0x1F6FC,
        0x1F7E0, 0x1F7EB,
        0x1F7F0, 0x1F7F0,
        0x1F900, 0x1F9FF,
        0x1FA70, 0x1FAFF,
    )

    /** One bit per code point below [BITSET_LIMIT], set when [WIDE_RANGES] covers it: 16 KiB. */
    private val WIDE_BITS: LongArray = LongArray(BITSET_LIMIT / Long.SIZE_BITS).also { bits ->
        for (pair in WIDE_RANGES.indices step 2) {
            for (cp in WIDE_RANGES[pair]..WIDE_RANGES[pair + 1]) {
                bits[cp ushr 6] = bits[cp ushr 6] or (1L shl cp)
            }
        }
    }
}
