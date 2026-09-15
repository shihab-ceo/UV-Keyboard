package com.example.translator

import android.util.Log
import android.util.LruCache
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Data representation of a supported translation language.
 */
data class Language(
    val code: String,
    val name: String,
    val nativeName: String
) {
    override fun toString(): String = "$nativeName ($name)"
}

/**
 * Robust, thread-safe asynchronous translation engine.
 *
 * Guarantees:
 * 1. Network API calls execute strictly on [Dispatchers.IO] via [withContext(Dispatchers.IO)].
 *    Prevents `NetworkOnMainThreadException` on all Android SDK levels.
 * 2. Complete exception isolation: all network timeouts, parse errors, and HTTP failures
 *    are caught safely; fallback to offline phrasebook or original text without crashing.
 * 3. 300ms debounced execution cancelling obsolete in-flight requests during rapid typing.
 * 4. Safe null-checking utility [commitSafely] for [InputConnection] transactions.
 */
class TranslatorEngine(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val client: OkHttpClient = defaultHttpClient()
) {

    companion object {
        private const val TAG = "TranslatorEngine"
        const val DEBOUNCE_DELAY_MS = 300L
        const val DEFAULT_SOURCE_LANG = "bn"
        const val DEFAULT_TARGET_LANG = "en"

        val SUPPORTED_LANGUAGES = listOf(
            Language("bn", "Bengali", "বাংলা"),
            Language("en", "English", "English"),
            Language("hi", "Hindi", "हिन्दी"),
            Language("ar", "Arabic", "العربية"),
            Language("es", "Spanish", "Español"),
            Language("fr", "French", "Français"),
            Language("de", "German", "Deutsch"),
            Language("ja", "Japanese", "日本語"),
            Language("zh", "Chinese", "中文"),
            Language("ur", "Urdu", "اردو"),
            Language("ru", "Russian", "Русский"),
            Language("pt", "Portuguese", "Português")
        )

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(8, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        // Offline phrasebook fallback for zero-latency / offline usage
        private val OFFLINE_BN_TO_EN = mapOf(
            "হ্যালো" to "Hello",
            "নমস্কার" to "Hello",
            "কেমন আছেন" to "How are you",
            "কেমন আছো" to "How are you",
            "আমি ভালো আছি" to "I am fine",
            "ভালো" to "Good",
            "ধন্যবাদ" to "Thank you",
            "অনেক ধন্যবাদ" to "Thanks a lot",
            "স্বাগতম" to "Welcome",
            "হ্যাঁ" to "Yes",
            "না" to "No",
            "শুভ সকাল" to "Good morning",
            "শুভ রাত্রি" to "Good night",
            "শুভ বিকাল" to "Good afternoon",
            "বিদায়" to "Goodbye",
            "দয়া করে" to "Please",
            "দুঃখিত" to "Sorry",
            "মাফ করবেন" to "Excuse me",
            "আপনি কোথায়" to "Where are you",
            "তুমি কোথায়" to "Where are you",
            "আপনার নাম কি" to "What is your name",
            "তোমার নাম কি" to "What is your name",
            "আমার নাম" to "My name is",
            "কি" to "What",
            "কে" to "Who",
            "কোথায়" to "Where",
            "কেন" to "Why",
            "কখন" to "When",
            "কিভাবে" to "How",
            "বাংলা" to "Bengali",
            "ইংরেজি" to "English",
            "ভালোবাসি" to "Love",
            "বন্ধু" to "Friend"
        )

        private val OFFLINE_EN_TO_BN = OFFLINE_BN_TO_EN.entries.associate { (k, v) ->
            v.lowercase() to k
        }

        /**
         * Safe null-checking utility for InputConnection commit and delete operations.
         * Protects against NullPointerException or DeadObjectException when interacting
         * with external applications.
         */
        @JvmStatic
        fun commitSafely(
            ic: InputConnection?,
            text: String,
            replaceLength: Int = 0
        ): Boolean {
            if (ic == null) return false
            return runCatching {
                if (replaceLength > 0) {
                    ic.deleteSurroundingText(replaceLength, 0)
                }
                if (text.isNotEmpty()) {
                    ic.commitText(text, 1)
                }
                true
            }.getOrElse { throwable ->
                Log.w(TAG, "Failed safe commit to InputConnection: ${throwable.message}")
                false
            }
        }
        /**
         * Safe direct commit:
         * Strictly verifies: if (currentInputConnection != null) { currentInputConnection.commitText(text, 1) }
         */
        @JvmStatic
        fun commitDirect(ic: InputConnection?, text: String): Boolean {
            if (ic != null) {
                return runCatching {
                    ic.commitText(text, 1)
                    true
                }.getOrElse { false }
            }
            return false
        }
    }

    private val networkJob = SupervisorJob()
    private val networkScope = CoroutineScope(Dispatchers.IO + networkJob + CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Network scope handled error: ${throwable.message}")
    })
    private var debounceJob: Job? = null

    // 256-entry LRU Cache for rapid repeated queries
    private val cache = LruCache<String, String>(256)

    fun getLanguageByCode(code: String): Language {
        return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
            ?: Language(code, code.uppercase(), code.uppercase())
    }

    /**
     * Debounced translation trigger (300ms delay).
     *
     * Threading guarantees:
     * - Debouncing and UI callback invocation occur strictly on [Dispatchers.Main].
     * - Network execution occurs strictly inside [networkScope] on [Dispatchers.IO].
     * - Exceptions are caught safely via runCatching and fallback translation is returned without crashing.
     */
    fun translateDebounced(
        text: String,
        sourceLang: String = DEFAULT_SOURCE_LANG,
        targetLang: String = DEFAULT_TARGET_LANG,
        scope: CoroutineScope? = null,
        onResult: (translated: String) -> Unit
    ): Job {
        debounceJob?.cancel()

        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onResult("")
            return Job().apply { complete() }
        }

        if (sourceLang.equals(targetLang, ignoreCase = true)) {
            onResult(text)
            return Job().apply { complete() }
        }

        val cacheKey = buildCacheKey(sourceLang, targetLang, trimmed)
        val cached = cache.get(cacheKey)
        if (cached != null) {
            onResult(cached)
            return Job().apply { complete() }
        }

        val activeScope = scope ?: CoroutineScope(Dispatchers.Main + SupervisorJob())
        val job = activeScope.launch(Dispatchers.Main) {
            val result = runCatching {
                delay(DEBOUNCE_DELAY_MS)
                // Strictly execute network on Dispatchers.IO via networkScope
                withContext(networkScope.coroutineContext) {
                    translateInternal(trimmed, sourceLang, targetLang)
                }
            }.getOrElse { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                Log.w(TAG, "translateDebounced exception caught safely: ${throwable.message}")
                findOfflineFallback(trimmed, sourceLang, targetLang) ?: trimmed
            }
            onResult(result)
        }
        debounceJob = job
        return job
    }

    /**
     * Suspend function for translation.
     * Strictly switches to [ioDispatcher] for network and OkHttp execution.
     * Uses runCatching to ensure complete thread and exception safety.
     */
    suspend fun translate(
        text: String,
        sourceLang: String = DEFAULT_SOURCE_LANG,
        targetLang: String = DEFAULT_TARGET_LANG
    ): String = withContext(ioDispatcher) {
        runCatching {
            translateInternal(text, sourceLang, targetLang)
        }.getOrElse { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }
            Log.w(TAG, "translate suspend exception caught safely", throwable)
            val trimmed = text.trim()
            findOfflineFallback(trimmed, sourceLang, targetLang) ?: trimmed
        }
    }

    /**
     * Internal network worker executed exclusively on background IO thread.
     */
    private fun translateInternal(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        if (sourceLang.equals(targetLang, ignoreCase = true)) return text

        val cacheKey = buildCacheKey(sourceLang, targetLang, trimmed)
        val cached = cache.get(cacheKey)
        if (cached != null) return cached

        val offline = findOfflineFallback(trimmed, sourceLang, targetLang)

        val translationResult = runCatching {
            val encodedText = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=${sourceLang}&tl=${targetLang}&dt=t&q=$encodedText"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val bodyString = resp.body?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        val parsed = parseTranslationResponse(bodyString)
                        if (parsed.isNotEmpty()) {
                            return@runCatching parsed
                        }
                    }
                }
            }
            null
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            Log.w(TAG, "Network translation call failed safely: ${throwable.message}")
            null
        }

        // Fallback to offline phrasebook or original text
        val fallback = translationResult ?: offline ?: trimmed
        cache.put(cacheKey, fallback)
        return fallback
    }

    private fun parseTranslationResponse(json: String): String {
        return try {
            val rootArray = JSONArray(json)
            val sentences = rootArray.optJSONArray(0) ?: return ""
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                val sentence = sentences.optJSONArray(i)
                val part = sentence?.optString(0)
                if (!part.isNullOrEmpty() && part != "null") {
                    sb.append(part)
                }
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun findOfflineFallback(text: String, sourceLang: String, targetLang: String): String? {
        val s = sourceLang.lowercase()
        val t = targetLang.lowercase()

        if (s == "bn" && t == "en") {
            OFFLINE_BN_TO_EN[text]?.let { return it }
        } else if (s == "en" && t == "bn") {
            OFFLINE_EN_TO_BN[text.lowercase()]?.let { return it }
        }
        return null
    }

    fun buildCacheKey(sourceLang: String, targetLang: String, text: String): String {
        return "${sourceLang.lowercase()}->${targetLang.lowercase()}:${text.trim()}"
    }

    fun cancelInFlight() {
        debounceJob?.cancel()
        debounceJob = null
    }

    fun clearCache() {
        cache.evictAll()
    }

    fun getCached(text: String, sourceLang: String, targetLang: String): String? {
        return cache.get(buildCacheKey(sourceLang, targetLang, text))
    }

    fun putInCache(text: String, sourceLang: String, targetLang: String, translated: String) {
        cache.put(buildCacheKey(sourceLang, targetLang, text), translated)
    }

    fun destroy() {
        cancelInFlight()
        networkJob.cancel()
        clearCache()
    }
}
