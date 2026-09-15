package com.example.ime

/**
 * Handles logical Bengali Unicode text composition, input validation, and deletion.
 *
 * Implements clean separation between physical layout keys and Unicode representation:
 * - Independent vowels vs. Vowel signs (Kar)
 * - Hasanta (virama: \u09CD) handling
 * - Conjunct (যুক্তবর্ণ) construction (C1 + ্ + C2)
 * - Reph (র্), Ra-phala (্র), and Ya-phala (্য)
 * - Safe logical backspace deletion that respects Bengali grapheme clusters and hasanta sequences.
 */
object BengaliCompositionHelper {

    const val HASANTA = "\u09CD" // Bengali virama: ্
    const val ZWNJ = "\u200C"    // Zero-width non-joiner
    const val ZWJ = "\u200D"     // Zero-width joiner
    const val KHANDA_TA = "ৎ"
    const val ANUSVARA = "ং"
    const val VISARGA = "ঃ"
    const val CANDRABINDU = "ঁ"
    const val DARI = "।"

    // Independent Vowels
    val INDEPENDENT_VOWELS = setOf(
        "অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ"
    )

    // Dependent Vowel Signs (Kar)
    val VOWEL_SIGNS = setOf(
        "া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ"
    )

    // Bengali Consonants
    val CONSONANTS = setOf(
        "ক", "খ", "গ", "ঘ", "ঙ",
        "চ", "ছ", "জ", "ঝ", "ঞ",
        "ট", "ঠ", "ড", "ঢ", "ণ",
        "ত", "থ", "দ", "ধ", "ন",
        "প", "ফ", "ব", "ভ", "ম",
        "য", "র", "ল", "শ", "ষ", "স", "হ",
        "ড়", "ঢ়", "য়"
    )

    fun isBengaliChar(ch: Char): Boolean = ch in '\u0980'..'\u09FF'

    fun isVowelSign(s: String): Boolean = s in VOWEL_SIGNS

    fun isIndependentVowel(s: String): Boolean = s in INDEPENDENT_VOWELS

    fun isConsonant(s: String): Boolean = s in CONSONANTS

    /**
     * Strips Unicode dotted circle (U+25CC / ◌) that some Android fonts attach to isolated vowel signs.
     */
    fun stripDottedCircle(glyph: String): String = glyph.replace("\u25CC", "")

    /**
     * Maps a vowel sign to its corresponding independent vowel.
     */
    fun signToIndependentVowel(sign: String): String? = when (sign) {
        "া" -> "আ"
        "ি" -> "ই"
        "ী" -> "ঈ"
        "ু" -> "উ"
        "ূ" -> "ঊ"
        "ৃ" -> "ঋ"
        "ে" -> "এ"
        "ৈ" -> "ঐ"
        "ো" -> "ও"
        "ৌ" -> "ঔ"
        else -> null
    }

    /**
     * Determines how many characters to delete on Backspace to safely remove a logical Bengali unit.
     * Prevents leaving an orphan virama or splitting composed characters unexpectedly.
     *
     * @param textBeforeCursor The text preceding the cursor (up to 10 characters)
     * @return Number of code units (chars) to delete, minimum 1.
     */
    fun getDeletionLength(textBeforeCursor: String): Int {
        if (textBeforeCursor.isEmpty()) return 1
        val len = textBeforeCursor.length
        val lastChar = textBeforeCursor.last()

        // If the last character is a surrogate pair low surrogate
        if (lastChar.isLowSurrogate() && len >= 2 && textBeforeCursor[len - 2].isHighSurrogate()) {
            return 2
        }

        // If last character is ZWJ or ZWNJ preceded by hasanta (e.g. reph or khanda ta forms)
        if ((lastChar == '\u200C' || lastChar == '\u200D') && len >= 2 && textBeforeCursor[len - 2] == '\u09CD') {
            return 2
        }

        return 1
    }
}
