package com.example.ime

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.KeyboardTheme
import com.example.R
import com.example.ime.suggestion.SuggestionCandidate

/**
 * Encapsulates the predictive Candidate View bar.
 * Provides real-time word completions/suggestions for English and all Bengali modes.
 */
class CandidateView(
    private val context: Context,
    private val container: LinearLayout,
    private val onCandidateSelected: (String) -> Unit
) {

    fun renderSuggestionCandidates(
        candidates: List<SuggestionCandidate>,
        theme: KeyboardTheme,
        fontSizeScale: Float = 1.0f
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(context)

        candidates.forEachIndexed { index, candidate ->
            val candidateView = try {
                inflater.inflate(R.layout.candidate_item, container, false) as TextView
            } catch (_: Exception) {
                TextView(context).apply {
                    setPadding(32, 10, 32, 10)
                }
            }

            val displayText = if (candidate.isAutocorrect) {
                "• ${candidate.text}"
            } else {
                candidate.text
            }

            candidateView.text = displayText
            candidateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14f * fontSizeScale).coerceIn(12f, 26f))

            if (index == 0) {
                // Highlight primary suggestion or autocorrect
                candidateView.setTextColor(theme.accentColor)
                val background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f
                    setColor(theme.keyColor)
                    setStroke(if (candidate.isAutocorrect) 3 else 2, theme.accentColor)
                }
                candidateView.background = background
            } else {
                candidateView.setTextColor(theme.textColor)
                val background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f
                    setColor(theme.keyColor)
                    setStroke(1, theme.sublabelColor)
                }
                candidateView.background = background
            }

            candidateView.setOnClickListener {
                onCandidateSelected(candidate.text)
            }

            container.addView(candidateView)
        }
    }

    fun updateSuggestions(
        prefix: String,
        layoutMode: String,
        theme: KeyboardTheme,
        fontSizeScale: Float = 1.0f
    ) {
        val candidates: List<String> = when {
            layoutMode == com.example.ThemeManager.LAYOUT_ENGLISH -> {
                BengaliEngine.getEnglishPredictions(prefix)
            }
            layoutMode == com.example.ThemeManager.LAYOUT_AVRO -> {
                val transliterated = BengaliEngine.phoneticTransliterate(prefix)
                val list = mutableListOf<String>()
                if (transliterated.isNotEmpty()) {
                    list.add(transliterated)
                }
                list.addAll(BengaliEngine.getPredictions(transliterated))
                list.distinct()
            }
            else -> {
                BengaliEngine.getPredictions(prefix)
            }
        }

        renderCandidates(candidates, theme, fontSizeScale)
    }

    fun renderCandidates(
        candidates: List<String>,
        theme: KeyboardTheme,
        fontSizeScale: Float = 1.0f
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(context)

        candidates.forEachIndexed { index, word ->
            val candidateView = try {
                inflater.inflate(R.layout.candidate_item, container, false) as TextView
            } catch (_: Exception) {
                TextView(context).apply {
                    setPadding(32, 10, 32, 10)
                }
            }

            candidateView.text = word
            candidateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14f * fontSizeScale).coerceIn(12f, 26f))

            if (index == 0) {
                // Highlight primary suggestion
                candidateView.setTextColor(theme.accentColor)
                val background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f
                    setColor(theme.keyColor)
                    setStroke(2, theme.accentColor)
                }
                candidateView.background = background
            } else {
                candidateView.setTextColor(theme.textColor)
                val background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f
                    setColor(theme.keyColor)
                    setStroke(1, theme.sublabelColor)
                }
                candidateView.background = background
            }

            candidateView.setOnClickListener {
                onCandidateSelected(word)
            }

            container.addView(candidateView)
        }
    }

    fun clear() {
        container.removeAllViews()
    }
}
