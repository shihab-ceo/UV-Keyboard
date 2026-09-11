package com.example.ime.suggestion

import android.content.Context
import com.example.clipboard.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PersonalLearningStore(
    context: Context,
    private val scope: CoroutineScope
) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.userWordDao()

    // Thread-safe in-memory cache for ultra-fast keystroke lookup without blocking IO
    private val wordFrequencyCache = ConcurrentHashMap<String, Int>()

    init {
        // Preload learned words into memory on startup
        scope.launch(Dispatchers.IO) {
            try {
                val topWords = dao.getTopWords(200)
                for (item in topWords) {
                    wordFrequencyCache[item.word] = item.frequency
                }
            } catch (_: Exception) {
                // Ignore DB warm-up error
            }
        }
    }

    /**
     * Records and increases frequency of a typed or chosen word.
     * Guaranteed NO-OP if isSafeField is false.
     */
    fun learnWord(word: String, language: String, isSafeField: Boolean, learningEnabled: Boolean) {
        if (!learningEnabled || !isSafeField) return
        val cleanWord = word.trim()
        if (cleanWord.length < 2 || cleanWord.length > 35) return

        // Do not learn emails, URLs, numbers, hashtags, @mentions
        if (cleanWord.contains("@") || cleanWord.contains("://") || cleanWord.contains("#") ||
            cleanWord.any { it.isDigit() } || cleanWord.contains("/") || cleanWord.contains("\\")
        ) {
            return
        }

        val currentFreq = wordFrequencyCache[cleanWord] ?: 0
        val newFreq = currentFreq + 1
        wordFrequencyCache[cleanWord] = newFreq

        scope.launch(Dispatchers.IO) {
            try {
                val existing = dao.getWord(cleanWord)
                val entity = UserWordEntity(
                    word = cleanWord,
                    language = language,
                    frequency = (existing?.frequency ?: 0) + 1,
                    lastUsedTimestamp = System.currentTimeMillis()
                )
                dao.insert(entity)
            } catch (_: Exception) {
                // Ignore local DB write failure
            }
        }
    }

    /**
     * Fast in-memory lookup for words matching prefix.
     */
    fun getLearnedMatches(prefix: String, limit: Int = 5): List<Pair<String, Int>> {
        if (prefix.isBlank()) return emptyList()
        val lower = prefix.lowercase()
        return wordFrequencyCache.entries
            .filter { it.key.lowercase().startsWith(lower) && it.key != prefix }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun getWordFrequency(word: String): Int {
        return wordFrequencyCache[word] ?: 0
    }

    fun clearAll() {
        wordFrequencyCache.clear()
        scope.launch(Dispatchers.IO) {
            try {
                dao.clearAll()
            } catch (_: Exception) {
            }
        }
    }
}
