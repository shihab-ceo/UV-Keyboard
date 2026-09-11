package com.example.ime.suggestion

import android.view.inputmethod.EditorInfo
import com.example.ime.BengaliEngine
import kotlin.math.max
import kotlin.math.min

class SuggestionEngine(
    private val learningStore: PersonalLearningStore? = null
) {

    companion object {
        const val AGGRESSIVENESS_CONSERVATIVE = "Conservative"
        const val AGGRESSIVENESS_BALANCED = "Balanced"
        const val AGGRESSIVENESS_AGGRESSIVE = "Aggressive"

        private const val THRESHOLD_CONSERVATIVE = 0.88f
        private const val THRESHOLD_BALANCED = 0.75f
        private const val THRESHOLD_AGGRESSIVE = 0.60f
    }

    /**
     * Checks if the active input field is safe for suggestions, autocorrect, and learning.
     */
    fun isSafeField(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return true

        val inputType = editorInfo.inputType
        val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
        val clazz = inputType and EditorInfo.TYPE_MASK_CLASS

        // Passwords and PINs are strictly unsafe
        if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == 16 /* TYPE_NUMBER_VARIATION_PASSWORD */ ||
            variation == 224 /* TYPE_TEXT_VARIATION_WEB_PASSWORD */
        ) {
            return false
        }

        // Check IME_FLAG_NO_PERSONALIZED_LEARNING
        if ((editorInfo.imeOptions and 0x1000000) != 0) {
            return false
        }

        return true
    }

    /**
     * Checks if the active input field should suppress autocorrect (e.g. URLs, Emails).
     */
    fun isAutocorrectPermittedField(editorInfo: EditorInfo?): Boolean {
        if (!isSafeField(editorInfo)) return false
        if (editorInfo == null) return true

        val inputType = editorInfo.inputType
        val variation = inputType and EditorInfo.TYPE_MASK_VARIATION

        if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            variation == EditorInfo.TYPE_TEXT_VARIATION_URI ||
            variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER
        ) {
            return false
        }

        return true
    }

    /**
     * Checks if a token should not be autocorrected (e.g. URLs, hashtags, mentions, code, numbers).
     */
    fun isTokenExemptFromAutocorrect(token: String): Boolean {
        if (token.isBlank()) return true
        if (token.startsWith("http://") || token.startsWith("https://") || token.startsWith("www.")) return true
        if (token.startsWith("@") || token.startsWith("#") || token.startsWith("/")) return true
        if (token.contains("@") || token.contains("://") || token.contains(".com") || token.contains(".org")) return true
        if (token.contains("\\") || token.contains("_") || token.contains("=") || token.contains("<") || token.contains(">")) return true
        if (token.any { it.isDigit() }) return true
        if (token.length <= 1) return true
        return false
    }

    /**
     * Detects if the string is predominantly Bengali Unicode.
     */
    fun isBengaliScript(text: String): Boolean {
        return text.any { it in '\u0980'..'\u09FF' }
    }

    /**
     * Generates intelligent suggestions and autocorrect candidates for the given query.
     */
    fun getSuggestions(
        query: String,
        precedingWord: String? = null,
        editorInfo: EditorInfo? = null,
        suggestionsEnabled: Boolean = true,
        autocorrectEnabled: Boolean = true,
        personalLearningEnabled: Boolean = true,
        aggressiveness: String = AGGRESSIVENESS_BALANCED,
        isAvroMode: Boolean = false,
        maxCandidates: Int = 7
    ): List<SuggestionCandidate> {
        if (!suggestionsEnabled || query.isBlank()) return emptyList()

        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Check security
        val isSafe = isSafeField(editorInfo)
        val canAutocorrectField = autocorrectEnabled && isAutocorrectPermittedField(editorInfo) && !isTokenExemptFromAutocorrect(trimmed)
        val isBengali = isBengaliScript(trimmed)

        val threshold = when (aggressiveness) {
            AGGRESSIVENESS_CONSERVATIVE -> THRESHOLD_CONSERVATIVE
            AGGRESSIVENESS_AGGRESSIVE -> THRESHOLD_AGGRESSIVE
            else -> THRESHOLD_BALANCED
        }

        val candidateMap = mutableMapOf<String, SuggestionCandidate>()

        // 1. Direct O(1) Typo Correction Table Lookup
        val directCorrection = if (isBengali) {
            LexiconDictionary.commonBengaliTypoMap[trimmed]
        } else {
            LexiconDictionary.commonEnglishTypoMap[trimmed.lowercase()]
        }

        if (directCorrection != null && directCorrection.isNotEmpty()) {
            val formatted = preserveCasing(trimmed, directCorrection)
            candidateMap[formatted] = SuggestionCandidate(
                text = formatted,
                score = 0.98f,
                isAutocorrect = canAutocorrectField,
                source = CandidateSource.TYPO_CORRECTION,
                originalQuery = trimmed
            )
        }

        // 2. Transliteration (for Latin script input, especially in Avro or mixed typing)
        if (!isBengali) {
            val lower = trimmed.lowercase()
            val mappedBangla = LexiconDictionary.commonTransliterationMap[lower]
            if (mappedBangla != null) {
                candidateMap[mappedBangla] = SuggestionCandidate(
                    text = mappedBangla,
                    score = if (isAvroMode) 0.92f else 0.82f,
                    isAutocorrect = canAutocorrectField && isAvroMode,
                    source = CandidateSource.TRANSLITERATION,
                    originalQuery = trimmed
                )
            }

            // Phonetic transliteration from existing BengaliEngine
            val transliterated = BengaliEngine.phoneticTransliterate(lower)
            if (transliterated.isNotEmpty() && transliterated != lower && !candidateMap.containsKey(transliterated)) {
                candidateMap[transliterated] = SuggestionCandidate(
                    text = transliterated,
                    score = if (isAvroMode) 0.90f else 0.80f,
                    isAutocorrect = false,
                    source = CandidateSource.TRANSLITERATION,
                    originalQuery = trimmed
                )
            }
        }

        // 3. Exact Dictionary Match
        val exactFreq = if (isBengali) {
            LexiconDictionary.bengaliWords[trimmed]
        } else {
            LexiconDictionary.englishWords[trimmed.lowercase()]
        }
        if (exactFreq != null) {
            candidateMap[trimmed] = SuggestionCandidate(
                text = trimmed,
                score = 0.90f + (exactFreq / 1000f),
                isAutocorrect = false,
                source = CandidateSource.EXACT,
                originalQuery = trimmed
            )
        }

        // 4. Personal Learning Matches
        if (isSafe && personalLearningEnabled && learningStore != null) {
            val learned = learningStore.getLearnedMatches(trimmed, limit = 4)
            for ((word, freq) in learned) {
                val score = 0.85f + min(freq * 0.02f, 0.12f)
                if (!candidateMap.containsKey(word) || candidateMap[word]!!.score < score) {
                    candidateMap[word] = SuggestionCandidate(
                        text = word,
                        score = score,
                        isAutocorrect = false,
                        source = CandidateSource.LEARNED,
                        originalQuery = trimmed
                    )
                }
            }
        }

        // 5. Context Predictions (Preceding word context)
        if (!precedingWord.isNullOrBlank()) {
            val contextWords = ContextPredictor.getContextCandidates(precedingWord)
            for (word in contextWords) {
                if (word.startsWith(trimmed, ignoreCase = true)) {
                    val formatted = if (!isBengali) preserveCasing(trimmed, word) else word
                    val score = 0.86f
                    if (!candidateMap.containsKey(formatted) || candidateMap[formatted]!!.score < score) {
                        candidateMap[formatted] = SuggestionCandidate(
                            text = formatted,
                            score = score,
                            isAutocorrect = false,
                            source = CandidateSource.CONTEXT,
                            originalQuery = trimmed
                        )
                    }
                }
            }
        }

        // 6. Prefix Matches from Dictionary
        if (isBengali) {
            val firstChar = trimmed.first()
            val bucket = LexiconDictionary.bengaliBuckets[firstChar] ?: emptyList()
            for (word in bucket) {
                if (word.startsWith(trimmed) && word != trimmed) {
                    val freq = LexiconDictionary.bengaliWords[word] ?: 50
                    val score = 0.65f + (freq / 600f)
                    if (!candidateMap.containsKey(word)) {
                        candidateMap[word] = SuggestionCandidate(
                            text = word,
                            score = score,
                            isAutocorrect = false,
                            source = CandidateSource.PREFIX,
                            originalQuery = trimmed
                        )
                    }
                }
            }
        } else {
            val firstChar = trimmed.first().lowercaseChar()
            val bucket = LexiconDictionary.englishBuckets[firstChar] ?: emptyList()
            val lower = trimmed.lowercase()
            for (word in bucket) {
                if (word.startsWith(lower) && word != lower) {
                    val formatted = preserveCasing(trimmed, word)
                    val freq = LexiconDictionary.englishWords[word] ?: 50
                    val score = 0.65f + (freq / 600f)
                    if (!candidateMap.containsKey(formatted)) {
                        candidateMap[formatted] = SuggestionCandidate(
                            text = formatted,
                            score = score,
                            isAutocorrect = false,
                            source = CandidateSource.PREFIX,
                            originalQuery = trimmed
                        )
                    }
                }
            }
        }

        // 7. Typo Corrections via Bounded Edit Distance (Damerau-Levenshtein <= 2)
        if (canAutocorrectField && trimmed.length >= 3 && candidateMap.size < maxCandidates + 2) {
            val collapsed = TypoDetector.collapseRepeatedCharacters(trimmed)
            if (collapsed != trimmed) {
                // Check if collapsed word exists in dictionary
                val colFreq = if (isBengali) LexiconDictionary.bengaliWords[collapsed] else LexiconDictionary.englishWords[collapsed.lowercase()]
                if (colFreq != null) {
                    val formatted = if (isBengali) collapsed else preserveCasing(trimmed, collapsed)
                    candidateMap[formatted] = SuggestionCandidate(
                        text = formatted,
                        score = 0.88f,
                        isAutocorrect = true,
                        source = CandidateSource.TYPO_CORRECTION,
                        originalQuery = trimmed
                    )
                }
            }

            // Search bounded typo candidates in corresponding word list
            val targetList = if (isBengali) {
                val firstChar = trimmed.first()
                LexiconDictionary.bengaliBuckets[firstChar] ?: emptyList()
            } else {
                val firstChar = trimmed.first().lowercaseChar()
                LexiconDictionary.englishBuckets[firstChar] ?: emptyList()
            }

            val lower = trimmed.lowercase()
            for (dictWord in targetList) {
                if (candidateMap.size >= maxCandidates + 4) break
                val dist = TypoDetector.boundedDamerauLevenshtein(lower, dictWord.lowercase(), maxDistance = 2)
                if (dist in 1..2) {
                    val freq = if (isBengali) LexiconDictionary.bengaliWords[dictWord] ?: 50 else LexiconDictionary.englishWords[dictWord] ?: 50
                    val distPenalty = dist * 0.14f
                    val score = 0.78f + (freq / 500f) - distPenalty
                    val isConfidentCorrection = (score >= threshold)
                    val formatted = if (isBengali) dictWord else preserveCasing(trimmed, dictWord)

                    if (!candidateMap.containsKey(formatted) || candidateMap[formatted]!!.score < score) {
                        candidateMap[formatted] = SuggestionCandidate(
                            text = formatted,
                            score = score,
                            isAutocorrect = isConfidentCorrection,
                            source = CandidateSource.TYPO_CORRECTION,
                            originalQuery = trimmed
                        )
                    }
                }
            }
        }

        // Always include exact user typed text if not present, so user retains full control
        if (!candidateMap.containsKey(trimmed)) {
            candidateMap[trimmed] = SuggestionCandidate(
                text = trimmed,
                score = 0.65f,
                isAutocorrect = false,
                source = CandidateSource.EXACT,
                originalQuery = trimmed
            )
        }

        // Sort candidates by score descending
        val sorted = candidateMap.values.sortedByDescending { it.score }.toMutableList()

        // If top candidate is autocorrect, ensure it surpasses the threshold
        if (sorted.isNotEmpty()) {
            val top = sorted.first()
            if (top.isAutocorrect && top.score < threshold) {
                sorted[0] = top.copy(isAutocorrect = false)
            }
        }

        return sorted.take(maxCandidates)
    }

    /**
     * Preserves capitalization style of input:
     * - "Hello" -> "Receive"
     * - "HELLO" -> "RECEIVE"
     * - "hello" -> "receive"
     */
    private fun preserveCasing(input: String, output: String): String {
        if (input.isEmpty() || output.isEmpty()) return output
        return when {
            input.all { it.isUpperCase() } -> output.uppercase()
            input.first().isUpperCase() -> output.replaceFirstChar { it.uppercaseChar() }
            else -> output.lowercase()
        }
    }
}
