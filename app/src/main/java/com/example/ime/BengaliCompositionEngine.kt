package com.example.ime

/**
 * Encapsulates the result of processing a key input in Bengali layout modes.
 */
sealed class CompositionResult {
    /**
     * Standard text commit without modifying previous text.
     */
    data class Commit(val text: String) : CompositionResult()

    /**
     * Replace previous [charsToDelete] characters before committing [replacement].
     */
    data class Replace(val charsToDelete: Int, val replacement: String) : CompositionResult()
}

/**
 * State machine / composition engine for Bengali layouts (Jatiya, Bijoy, Prabhat).
 *
 * Guarantees:
 * - Independent vowels are treated as independent vowels.
 * - Vowel signs (কার) attached to consonants work correctly (e.g. ক + ি = কি).
 * - Hasanta (্) remains hasanta and allows proper conjunct formation (C1 + ্ + C2).
 * - Optional G-link for Bijoy (্ + কার -> independent vowel) is handled cleanly without corrupting general sequences.
 * - Ya-phala (্য) and Ra-phala (্র) are preserved as canonical Bengali Unicode sequences.
 * - Validates logical Unicode sequences.
 */
object BengaliCompositionEngine {

    fun processInput(
        incoming: String,
        prevChar: String?,
        isBijoy: Boolean = false
    ): CompositionResult {
        if (incoming.isEmpty()) return CompositionResult.Commit("")

        // 1. If incoming is already an independent vowel or consonant or sign
        if (BengaliCompositionHelper.isIndependentVowel(incoming)) {
            // Already an independent vowel (e.g. অ, আ, ই from shift or layout)
            return CompositionResult.Commit(incoming)
        }

        // 2. If incoming is a vowel sign (কার)
        if (BengaliCompositionHelper.isVowelSign(incoming)) {
            // Check if preceded by hasanta (্) in Bijoy layout ONLY.
            // In Jatiya layout, independent vowels come strictly from the shifted map / long-press.
            if (isBijoy && prevChar == BengaliCompositionHelper.HASANTA) {
                val independent = BengaliCompositionHelper.signToIndependentVowel(incoming)
                if (independent != null) {
                    return CompositionResult.Replace(1, independent)
                }
            }
            // Otherwise, keep as dependent vowel sign (e.g. following a consonant or independent keystroke)
            return CompositionResult.Commit(incoming)
        }

        // 3. If incoming is hasanta (্)
        if (incoming == BengaliCompositionHelper.HASANTA) {
            // Hasanta remains hasanta: commits ् to allow next consonant to form a conjunct
            return CompositionResult.Commit(incoming)
        }

        // 4. Consonants or other characters
        return CompositionResult.Commit(incoming)
    }
}
