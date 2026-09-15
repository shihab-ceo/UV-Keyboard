package com.example.ime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.inputmethod.InputConnection
import android.widget.Toast

/**
 * Production-grade VoiceInputManager for UV Keyboard IME.
 *
 * Design & Architecture:
 * 1. ELIMINATES LOOPING LOGIC & SYSTEM SOUNDS:
 *    - Removes all background restart loops, retry handlers, and auto-restarts in
 *      `onEndOfSpeech`/`onError`. This permanently stops repetitive system beep tones
 *      and microphone UI flickering.
 * 2. NATIVE SPEECH DICTATION INTENT:
 *    - Dynamically configures the active layout locale:
 *      - Bengali layouts (Jatiya, Bijoy, Avro, Prabhat) -> "bn-BD"
 *      - English layout -> "en-US"
 *    - Configures RecognizerIntent with:
 *      - EXTRA_LANGUAGE_MODEL = LANGUAGE_MODEL_FREE_FORM
 *      - EXTRA_PARTIAL_RESULTS = true
 *      - EXTRA_LANGUAGE & EXTRA_LANGUAGE_PREFERENCE = active locale
 *      - EXTRA_MAX_RESULTS = 1
 * 3. CLEAN SINGLE COMMIT:
 *    - Extracts `matches[0]` from RESULTS_RECOGNITION and commits text to InputConnection
 *      EXACTLY ONCE with natural trailing space formatting upon dictation completion.
 *    - Handles errors and cancellation gracefully without leaving the keyboard in a frozen state.
 */
class VoiceInputManager(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onPartialRecognized: ((String) -> Unit)? = null,
    private val inputConnectionProvider: (() -> InputConnection?)? = null
) {

    companion object {
        private const val TAG = "VoiceInputManager"

        /**
         * Determines if the specified layout name corresponds to ANY Bengali layout:
         * Jatiya, Bijoy, Avro, or Prabhat.
         */
        fun isBengaliLayout(layoutName: String?): Boolean {
            if (layoutName.isNullOrBlank()) return true // Default layout in UV Keyboard is Bengali
            val normalized = layoutName.trim().uppercase()
            return when (normalized) {
                "ENGLISH", "EN", "EN-US", "EN-GB" -> false
                "JATIYA", "BIJOY", "AVRO", "PRABHAT" -> true
                else -> !normalized.contains("ENGL")
            }
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null

    @Volatile
    var isListening: Boolean = false
        private set

    val isDictationActive: Boolean
        get() = isListening

    val isListeningRequested: Boolean
        get() = isListening

    /**
     * RecognitionListener handling the standard native speech recognition lifecycle.
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            onListeningStateChanged(true)
        }

        override fun onBeginningOfSpeech() {
            // User began speaking
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Volume level update
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            // Speech ended. Waiting for final results.
        }

        override fun onError(error: Int) {
            Log.w(TAG, "SpeechRecognizer onError: $error")
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                Toast.makeText(context, "Microphone permission required for voice typing", Toast.LENGTH_SHORT).show()
            }
            // Cleanly end session and restore mic key UI to idle without looping restarts
            cleanup()
            isListening = false
            onListeningStateChanged(false)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val finalText = matches?.firstOrNull()?.trim()

            // Commit recognized text EXACTLY ONCE with natural trailing spacing
            if (!finalText.isNullOrEmpty()) {
                val committedText = if (finalText.endsWith(" ")) finalText else "$finalText "
                val ic = inputConnectionProvider?.invoke()
                if (ic != null) {
                    ic.finishComposingText()
                    ic.commitText(committedText, 1)
                } else {
                    onTextRecognized(committedText)
                }
            }

            // Cleanly finish session without any restart loops or repetitive beeps
            cleanup()
            isListening = false
            onListeningStateChanged(false)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Preview text only: NEVER commit to input connection here to avoid double-texting
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim()
            if (!partial.isNullOrEmpty()) {
                onPartialRecognized?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Starts listening by dynamically detecting whether the layout is
     * any Bengali layout (Jatiya, Bijoy, Avro, Prabhat) or English.
     */
    fun startListeningForLayout(layoutName: String?) {
        val isBengali = isBengaliLayout(layoutName)
        startListening(isBengali)
    }

    /**
     * Launches the native platform speech recognition intent cleanly.
     */
    fun startListening(isBengali: Boolean) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Voice recognition service unavailable", Toast.LENGTH_SHORT).show()
            isListening = false
            onListeningStateChanged(false)
            return
        }

        cleanup()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }

            val locale = if (isBengali) "bn-BD" else "en-US"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, locale)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            onListeningStateChanged(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch native speech recognition: ${e.message}", e)
            cleanup()
            isListening = false
            onListeningStateChanged(false)
        }
    }

    /**
     * Cleanly stops voice input and releases recognition resources.
     */
    fun stopListening() {
        cleanup()
        isListening = false
        onListeningStateChanged(false)
    }

    private fun cleanup() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }
}
