package com.example.ime.suggestion

enum class CandidateSource {
    EXACT,
    PREFIX,
    TYPO_CORRECTION,
    TRANSLITERATION,
    LEARNED,
    CONTEXT
}

data class SuggestionCandidate(
    val text: String,
    val score: Float,
    val isAutocorrect: Boolean = false,
    val source: CandidateSource = CandidateSource.PREFIX,
    val originalQuery: String = ""
)
