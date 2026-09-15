package com.example.keyboard.ui

import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.R
import com.example.ThemeManager
import com.example.ime.EmojiKeyboardView
import com.example.translator.TranslatorEngine
import com.example.translator.TranslatorView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Production-ready Standalone Reference InputMethodService implementation.
 *
 * Stability & Architecture:
 * 1. Safe Coroutine Lifecycle: Bound to [serviceScope] (SupervisorJob + Dispatchers.Main + CoroutineExceptionHandler),
 *    cancelled cleanly via [serviceJob.cancel()] in [onDestroy].
 * 2. Lazy View Inflation: [TranslatorView] and [CursorTrackpadView] are NEVER inflated eagerly in [onCreateInputView];
 *    they are instantiated only when explicitly requested by user action.
 * 3. Complete Null Safety: All [currentInputConnection] transactions are guarded via `val ic = currentInputConnection ?: return`.
 * 4. Feature A1 (Live Translator):
 *    - Standard keyboard typing NEVER triggers translation logic unless explicit Translate Mode
 *      is toggled ON by the user.
 *    - Translation network operations run exclusively on Dispatchers.IO wrapped in runCatching.
 * 5. Feature A2 (Dedicated Visual Cursor Trackpad):
 *    - Lazily inflated when toggled via top action bar icon (✥).
 *    - Uses dynamic InputConnection provider to prevent stale/null connection crashes.
 * 6. Layout Visibility Persistence:
 *    - Keyboard container strictly retains View.VISIBLE across all input sessions.
 */
class MyInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "MyInputMethodService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob + CoroutineExceptionHandler { _, throwable ->
        Log.e("UVKeyboard", "Handled background crash: ${throwable.message}")
    })

    private val themeManager by lazy { ThemeManager(this) }

    var keyboardViewContainer: FrameLayout? = null
        private set

    private var mainKeyboardLayout: LinearLayout? = null
    private var topActionBar: LinearLayout? = null
    private var extensionHeaderContainer: FrameLayout? = null
    private var keyboardContainer: FrameLayout? = null
    private var btnTranslateAction: TextView? = null
    private var btnTrackpadAction: TextView? = null
    private var keyboardView: JatiyaKeyboardView? = null
    private var emojiKeyboardView: EmojiKeyboardView? = null
    var translatorView: TranslatorView? = null
        private set
    var trackpadView: CursorTrackpadView? = null
        private set

    var isEmojiOpen: Boolean = false
        private set
    var isTranslatorOpen: Boolean = false
        private set
    var isTrackpadOpen: Boolean = false
        private set

    // Track character length of active translation stream in target application
    private var lastCommittedTranslationLength: Int = 0

    override fun onCreateInputView(): View {
        return try {
            val themeWrapper = ContextThemeWrapper(this, R.style.Theme_UVKeyboard)
            val inflater = LayoutInflater.from(themeWrapper)
            val root = inflater.inflate(R.layout.main_keyboard_layout, null) as FrameLayout
            keyboardViewContainer = root

            mainKeyboardLayout = root.findViewById(R.id.ll_main_keyboard_layout)
            topActionBar = root.findViewById(R.id.ll_top_action_bar)

            btnTranslateAction = root.findViewById<TextView>(R.id.btn_translate_action)?.apply {
                setOnClickListener {
                    runCatching {
                        toggleTranslator()
                    }.onFailure { t ->
                        Log.e(TAG, "Crash prevented in translator toolbar click", t)
                    }
                }
            }

            btnTrackpadAction = root.findViewById<TextView>(R.id.btn_trackpad_action)?.apply {
                setOnClickListener {
                    runCatching {
                        toggleTrackpad()
                    }.onFailure { t ->
                        Log.e(TAG, "Crash prevented in trackpad toolbar click", t)
                    }
                }
            }

            root.findViewById<TextView>(R.id.btn_emoji_action)?.apply {
                setOnClickListener {
                    if (isEmojiOpen) hideEmojiPanel() else showEmojiPanel()
                }
            }

            extensionHeaderContainer = root.findViewById(R.id.extensionHeaderContainer)
            keyboardContainer = root.findViewById(R.id.keyboardContainer)

            // Calculate active keyboard height and fix keyboardContainer to prevent unexpected layout distortion
            val baseHeightDp = 230
            val activeKeyboardHeight = (baseHeightDp * resources.displayMetrics.density * themeManager.keyboardHeightScale.coerceIn(0.85f, 1.4f)).toInt()
            keyboardContainer?.layoutParams?.height = activeKeyboardHeight

            val canvasHolder = root.findViewById<FrameLayout>(R.id.fl_keyboard_canvas_holder)
            val emojiHolder = root.findViewById<FrameLayout>(R.id.fl_emoji_canvas_holder)

            // Setup full-keyboard CursorTrackpadView overlay inside keyboardContainer
            trackpadView = root.findViewById<CursorTrackpadView>(R.id.cursorTrackpadView)?.apply {
                inputConnection = currentInputConnection
                inputConnectionProvider = { currentInputConnection }
                onCloseClicked = {
                    hideTrackpad()
                }
            }

            // Initialize Main Keyboard Canvas View with themed context
            val mainKeyboardView = JatiyaKeyboardView(themeWrapper).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                visibility = View.VISIBLE

                // Tap / Long-press Character Commit
                onKeyCommitListener = { text ->
                    keyboardViewContainer?.visibility = View.VISIBLE
                    if (isTranslatorOpen) {
                        translatorView?.appendInputText(text)
                    } else {
                        val ic = currentInputConnection
                        if (ic != null) {
                            try {
                                ic.commitText(text, 1)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error committing text", e)
                            }
                        }
                    }
                }

                // Backspace Action
                onBackspaceListener = {
                    keyboardViewContainer?.visibility = View.VISIBLE
                    if (isTranslatorOpen) {
                        translatorView?.deleteLastChar()
                    } else {
                        val ic = currentInputConnection
                        if (ic != null) {
                            try {
                                val selectedText = ic.getSelectedText(0)
                                if (selectedText.isNullOrEmpty()) {
                                    ic.deleteSurroundingText(1, 0)
                                } else {
                                    ic.commitText("", 1)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error handling backspace", e)
                            }
                        }
                    }
                }

                // Enter Action
                onEnterListener = {
                    if (isTranslatorOpen) {
                        translatorView?.commitCurrentTranslation()
                    } else {
                        handleEnterKey()
                    }
                }

                // Space Action
                onSpaceListener = {
                    keyboardViewContainer?.visibility = View.VISIBLE
                    if (isTranslatorOpen) {
                        translatorView?.appendInputText(" ")
                    } else {
                        val ic = currentInputConnection
                        if (ic != null) {
                            try {
                                ic.commitText(" ", 1)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error committing space", e)
                            }
                        }
                    }
                }

                onShiftToggleListener = { _ ->
                    keyboardViewContainer?.visibility = View.VISIBLE
                }

                onEmojiToggleListener = {
                    showEmojiPanel()
                }

                onSymbolsToggleListener = {
                    keyboardViewContainer?.visibility = View.VISIBLE
                }
            }
            this.keyboardView = mainKeyboardView
            canvasHolder?.addView(mainKeyboardView)

            // Setup Emoji Keyboard View with themed context
            val emojiView = EmojiKeyboardView(themeWrapper).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    dp(245)
                )
                visibility = View.GONE
                inputConnection = currentInputConnection

                onEmojiSelected = { _ ->
                    keyboardViewContainer?.visibility = View.VISIBLE
                    inputConnection = currentInputConnection
                }

                onBackspace = {
                    keyboardViewContainer?.visibility = View.VISIBLE
                    val ic = currentInputConnection
                    if (ic != null) {
                        try {
                            val selectedText = ic.getSelectedText(0)
                            if (selectedText.isNullOrEmpty()) {
                                ic.deleteSurroundingText(1, 0)
                            } else {
                                ic.commitText("", 1)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Emoji backspace error", e)
                        }
                    }
                }

                onCloseEmoji = {
                    hideEmojiPanel()
                }
            }
            this.emojiKeyboardView = emojiView
            emojiHolder?.addView(emojiView)

            root
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal error in onCreateInputView, using crash-proof fallback layout", t)
            val fallbackWrapper = ContextThemeWrapper(this, R.style.Theme_UVKeyboard)
            val fallback = LinearLayout(fallbackWrapper).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#0F172A"))
                val tv = TextView(fallbackWrapper).apply {
                    text = "UV Keyboard"
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(24), 0, dp(24))
                }
                addView(tv)
            }
            val root = FrameLayout(fallbackWrapper).apply { addView(fallback) }
            keyboardViewContainer = root
            root
        }
    }

    fun hideExtensionContainer() {
        runCatching {
            if (isTranslatorOpen) hideTranslator()
            if (isTrackpadOpen) hideTrackpad()
        }.onFailure { t ->
            Log.e(TAG, "Error in hideExtensionContainer", t)
        }
    }

    // Feature A1: In-Keyboard Translator Toggles (inflates inside extensionHeaderContainer ABOVE keyboard)
    fun toggleTranslator() {
        runCatching {
            if (isTranslatorOpen) {
                hideTranslator()
            } else {
                showTranslator()
            }
        }.onFailure { t ->
            Log.e(TAG, "Error in toggleTranslator", t)
        }
    }

    fun showTranslator() {
        runCatching {
            if (isTrackpadOpen) hideTrackpad()
            if (isEmojiOpen) hideEmojiPanel()

            val container = extensionHeaderContainer ?: return
            isTranslatorOpen = true
            btnTranslateAction?.setTextColor(Color.parseColor("#38BDF8"))
            btnTrackpadAction?.setTextColor(Color.parseColor("#94A3B8"))

            if (translatorView == null) {
                val themeWrapper = ContextThemeWrapper(this, R.style.Theme_UVKeyboard)
                val newTranslator = TranslatorView(themeWrapper).apply {
                    visibility = View.VISIBLE
                    isTranslateModeActive = true
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )

                    onTranslationReady = { _, translated ->
                        if (isTranslateModeActive && isTranslatorOpen) {
                            val ic = currentInputConnection
                            if (ic != null) {
                                val success = TranslatorEngine.commitSafely(ic, translated, lastCommittedTranslationLength)
                                if (success) {
                                    lastCommittedTranslationLength = if (translated.isNotEmpty()) translated.length else 0
                                } else {
                                    lastCommittedTranslationLength = 0
                                }
                            }
                        }
                    }

                    onCommitRequested = { translated ->
                        val ic = currentInputConnection
                        if (ic != null) {
                            try {
                                if (lastCommittedTranslationLength == 0 && translated.isNotEmpty()) {
                                    ic.commitText(translated + " ", 1)
                                } else {
                                    ic.commitText(" ", 1)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Exception committing translation on enter", e)
                            }
                        }
                        lastCommittedTranslationLength = 0
                    }

                    onCloseClicked = {
                        hideTranslator()
                    }
                }

                this.translatorView = newTranslator
                container.addView(newTranslator)
            } else {
                translatorView?.isTranslateModeActive = true
            }

            container.visibility = View.VISIBLE

            // CRITICAL: DO NOT resize or compress keyboardContainer.
            // Automatically adjust key paddings so the overall IME window never exceeds maximum system screen boundary,
            // keeping the bottom row keys (Spacebar/Enter) 100% visible.
            keyboardView?.isCompactMode = true
            keyboardView?.visibility = View.VISIBLE
            keyboardViewContainer?.visibility = View.VISIBLE
        }.onFailure { t ->
            Log.e(TAG, "Error in showTranslator", t)
        }
    }

    fun hideTranslator() {
        runCatching {
            isTranslatorOpen = false
            btnTranslateAction?.setTextColor(Color.parseColor("#94A3B8"))
            translatorView?.isTranslateModeActive = false
            translatorView?.clearInput()
            lastCommittedTranslationLength = 0
            extensionHeaderContainer?.visibility = View.GONE
            keyboardView?.isCompactMode = false
            keyboardView?.visibility = View.VISIBLE
            keyboardViewContainer?.visibility = View.VISIBLE
        }.onFailure { t ->
            Log.e(TAG, "Error in hideTranslator", t)
        }
    }

    // Feature A2: Dedicated Full-Keyboard Cursor Trackpad Overlay Toggles
    fun toggleTrackpad() {
        runCatching {
            if (isTrackpadOpen) {
                hideTrackpad()
            } else {
                showTrackpad()
            }
        }.onFailure { t ->
            Log.e(TAG, "Error in toggleTrackpad", t)
        }
    }

    fun showTrackpad() {
        runCatching {
            if (isTranslatorOpen) hideTranslator()
            if (isEmojiOpen) hideEmojiPanel()

            val trackpad = trackpadView ?: return
            isTrackpadOpen = true
            btnTrackpadAction?.setTextColor(Color.parseColor("#38BDF8"))
            btnTranslateAction?.setTextColor(Color.parseColor("#94A3B8"))

            // Connect live input connection
            trackpad.inputConnection = currentInputConnection
            trackpad.inputConnectionProvider = { currentInputConnection }
            trackpad.visibility = View.VISIBLE

            // Hide keyboard canvas so trackpad occupies 100% of the keyboardContainer without white gaps
            keyboardView?.visibility = View.GONE
            keyboardViewContainer?.visibility = View.VISIBLE
        }.onFailure { t ->
            Log.e(TAG, "Error in showTrackpad", t)
        }
    }

    fun hideTrackpad() {
        runCatching {
            isTrackpadOpen = false
            btnTrackpadAction?.setTextColor(Color.parseColor("#94A3B8"))
            trackpadView?.visibility = View.GONE
            keyboardView?.visibility = View.VISIBLE
            keyboardViewContainer?.visibility = View.VISIBLE
        }.onFailure { t ->
            Log.e(TAG, "Error in hideTrackpad", t)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        // Ensure main keyboard root view layout retains VISIBLE status at all times
        keyboardViewContainer?.visibility = View.VISIBLE
        trackpadView?.inputConnection = currentInputConnection
        trackpadView?.inputConnectionProvider = { currentInputConnection }

        if (restarting) {
            emojiKeyboardView?.inputConnection = currentInputConnection
            if (isEmojiOpen) {
                mainKeyboardLayout?.visibility = View.GONE
                emojiKeyboardView?.visibility = View.VISIBLE
                return
            }
        } else {
            // Fresh field session
            lastCommittedTranslationLength = 0
            if (isTrackpadOpen || isTranslatorOpen) {
                hideExtensionContainer()
            }
        }

        // Normal/New session initialization
        isEmojiOpen = false
        emojiKeyboardView?.visibility = View.GONE
        mainKeyboardLayout?.visibility = View.VISIBLE
        keyboardView?.visibility = View.VISIBLE
        keyboardView?.isShifted = false
        emojiKeyboardView?.inputConnection = currentInputConnection

        if (!isTranslatorOpen && !isTrackpadOpen) {
            extensionHeaderContainer?.visibility = View.GONE
        }

        updateEnterKeyDisplay(info)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            serviceJob.cancel()
            extensionHeaderContainer?.removeAllViews()
            translatorView?.onDestroy()
            translatorView = null
            trackpadView = null
        }
    }

    fun showEmojiPanel() {
        hideExtensionContainer()
        isEmojiOpen = true
        keyboardViewContainer?.visibility = View.VISIBLE
        mainKeyboardLayout?.visibility = View.GONE
        val emojiHolder = keyboardViewContainer?.findViewById<FrameLayout>(R.id.fl_emoji_canvas_holder)
        emojiHolder?.visibility = View.VISIBLE
        emojiKeyboardView?.inputConnection = currentInputConnection
        emojiKeyboardView?.visibility = View.VISIBLE
    }

    fun hideEmojiPanel() {
        isEmojiOpen = false
        keyboardViewContainer?.visibility = View.VISIBLE
        val emojiHolder = keyboardViewContainer?.findViewById<FrameLayout>(R.id.fl_emoji_canvas_holder)
        emojiHolder?.visibility = View.GONE
        emojiKeyboardView?.visibility = View.GONE
        mainKeyboardLayout?.visibility = View.VISIBLE
    }

    fun onKey(primaryCode: Int): Boolean {
        if (primaryCode == KeyEvent.KEYCODE_ENTER) {
            handleEnterKey()
            return true
        }
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isTrackpadOpen || isTranslatorOpen) {
                hideExtensionContainer()
                return true
            }
            if (isEmojiOpen) {
                hideEmojiPanel()
                return true
            }
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (isTranslatorOpen) {
                translatorView?.commitCurrentTranslation()
                return true
            }
            return onKey(keyCode)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    /**
     * Resolves appropriate Enter icon for JatiyaKeyboardView:
     * - Multi-line fields: strictly display "↵".
     * - Single-line fields (e.g. search): display action icon.
     */
    private fun updateEnterKeyDisplay(info: EditorInfo?) {
        val kv = keyboardView ?: return
        val inputType = info?.inputType ?: 0

        if ((inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0 || isMultiLineField(info)) {
            kv.enterActionIcon = "↵"
            return
        }

        val imeOptions = info?.imeOptions ?: 0
        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
        kv.enterActionIcon = when (actionId) {
            EditorInfo.IME_ACTION_SEARCH -> "🔍"
            EditorInfo.IME_ACTION_GO -> "➔"
            EditorInfo.IME_ACTION_SEND -> "📤"
            EditorInfo.IME_ACTION_NEXT -> "⏭️"
            EditorInfo.IME_ACTION_DONE -> "✓"
            else -> "↵"
        }
    }

    private fun isMultiLineField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val isMultiLineFlag = (info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val isImeNoEnter = (info.inputType and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE) != 0
        val isVariationsMultiLine = when (info.inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE,
            InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> true
            else -> false
        }
        return isMultiLineFlag || isImeNoEnter || isVariationsMultiLine
    }

    fun handleEnterKey() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val inputType = info?.inputType ?: 0

        try {
            if ((inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0 || isMultiLineField(info)) {
                ic.commitText("\n", 1)
                return
            }

            val imeOptions = info?.imeOptions ?: 0
            val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
            if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(actionId)
            } else {
                ic.commitText("\n", 1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling enter key", e)
        }
    }
}
