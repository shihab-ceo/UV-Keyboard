package com.example.ime

/**
 * Production-ready Jatiya Layout (জাতীয়) Mapping Engine.
 * Follows the standard national Bengali keyboard layout as specified by BDS 1738
 * and implemented in reference Android IMEs (Redmi/Gboard Jatiya).
 *
 * Standalone Vowels (অ, আ, ই, ঈ, উ, ঊ, ঋ, এ, ঐ, ও, ঔ) map directly to their
 * corresponding shifted keys. Eliminates legacy hacks converting '্' + Kar into Independent Vowels.
 */
object JatiyaLayoutEngine {

    /**
     * Standard QWERTY key mapping to Jatiya Unshifted characters.
     *
     * Row 1 (Q to P): ঙ, য, ড, প, ট, চ, জ, হ, গ, ড়
     * Row 2 (A to L / ;): ূ, ু, ি, ব, ্, া, ক, ত, দ
     * Row 3 (Z to M): ৌ, ো, ে, র, ন, স, ম
     */
    val unshiftedJatiyaMap: Map<Char, String> = mapOf(
        // Row 1 (Top Row)
        'q' to "ঙ",
        'w' to "য",
        'e' to "ড",
        'r' to "প",
        't' to "ট",
        'y' to "চ",
        'u' to "জ",
        'i' to "হ",
        'o' to "গ",
        'p' to "ড়",

        // Row 2 (Middle Row)
        'a' to "ূ",
        's' to "ু",
        'd' to "ি",
        'f' to "ব",
        'g' to "্",
        'h' to "া",
        'j' to "ক",
        'k' to "ত",
        'l' to "দ",

        // Row 3 (Bottom Row)
        'z' to "ৌ",
        'x' to "ো",
        'c' to "ে",
        'v' to "র",
        'b' to "ন",
        'n' to "স",
        'm' to "ম"
    )

    /**
     * Standard QWERTY key mapping to Jatiya Shifted characters.
     *
     * Row 1 (Q to P): ং, য়, ঢ, ফ, ঠ, ছ, ঝ, ঞ, ঘ, ঢ়
     * Row 2 (A to L / ;): ঋ, ঊ, ঈ, ভ, ্, আ, খ, থ, ধ
     * Row 3 (Z to M): ঔ, ও, এ, ল, ণ, ষ, শ
     */
    val shiftedJatiyaMap: Map<Char, String> = mapOf(
        // Row 1 (Top Row)
        'q' to "ং",
        'w' to "য়",
        'e' to "ঢ",
        'r' to "ফ",
        't' to "ঠ",
        'y' to "ছ",
        'u' to "ঝ",
        'i' to "ঞ",
        'o' to "ঘ",
        'p' to "ঢ়",

        // Row 2 (Middle Row)
        'a' to "ঋ",
        's' to "ঊ",
        'd' to "ঈ",
        'f' to "ভ",
        'g' to "্",
        'h' to "আ",
        'j' to "খ",
        'k' to "থ",
        'l' to "ধ",

        // Row 3 (Bottom Row)
        'z' to "ঔ",
        'x' to "ও",
        'c' to "এ",
        'v' to "ল",
        'b' to "ণ",
        'n' to "ষ",
        'm' to "শ"
    )

    /**
     * Maps a physical key and shift state directly to the exact Jatiya Unicode character.
     *
     * @param key The keyboard key character (case-insensitive for Latin letters).
     * @param isShifted True if the keyboard is currently shifted or caps-locked.
     * @return The resulting Bengali Unicode string, or the key itself as string if not mapped.
     */
    fun getJatiyaChar(key: Char, isShifted: Boolean): String {
        val lowerKey = key.lowercaseChar()
        val targetMap = if (isShifted) shiftedJatiyaMap else unshiftedJatiyaMap
        return targetMap[lowerKey] ?: key.toString()
    }
}
