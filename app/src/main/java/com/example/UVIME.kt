package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import com.example.clipboard.ClipboardItem
import com.example.ime.BengaliEngine
import com.example.ime.BengaliLayouts
import com.example.ime.CandidateView
import com.example.ime.KeyType
import com.example.ime.KeyboardKey
import com.example.ime.KeyboardLayoutMapper
import com.example.ime.VoiceInputHelper
import com.example.ime.suggestion.PersonalLearningStore
import com.example.ime.suggestion.SuggestionCandidate
import com.example.ime.suggestion.SuggestionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UVIME : InputMethodService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var themeManager: ThemeManager
    private lateinit var clipboardHelper: ClipboardManagerHelper
    private var voiceHelper: VoiceInputHelper? = null
    private var candidateViewHelper: CandidateView? = null

    // UI elements
    private var rootView: View? = null
    private var ivBgImage: ImageView? = null
    private var vBgOverlay: View? = null
    private var llCandidateBar: LinearLayout? = null
    private var btnSwitchMode: TextView? = null
    private var btnClipboardQuick: TextView? = null
    private var btnVoiceMic: TextView? = null
    private var llCandidatesContainer: LinearLayout? = null
    private var llClipboardPanel: LinearLayout? = null
    private var llClipboardItems: LinearLayout? = null
    private var llEmojiPanel: LinearLayout? = null
    private var llEmojiItems: LinearLayout? = null
    private var llKeyboardRows: LinearLayout? = null

    // State
    private var currentMode = ThemeManager.LAYOUT_JATIYA
    private var isShifted = false // Default to unshifted state on startup
    private var isCapsLock = false
    private var lastShiftTapTime = 0L
    private val DOUBLE_TAP_TIMEOUT = 350L
    private var isSymbols = false
    private var isBengaliNumbers = false
    private var isEmojiOpen = false
    private var isClipboardOpen = false
    private val phoneticBuffer = StringBuilder()
    private val currentWordBuffer = StringBuilder()

    // Smart Suggestion & Autocorrect Engine
    private lateinit var learningStore: PersonalLearningStore
    private lateinit var suggestionEngine: SuggestionEngine
    private var currentCandidates: List<SuggestionCandidate> = emptyList()
    private var suggestionGenerationId: Long = 0L
    private var lastAutocorrectOriginal: String? = null
    private var lastAutocorrectReplacement: String? = null

    // Feedback
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    // Repeat handler for Backspace
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        themeManager = ThemeManager(this)
        clipboardHelper = ClipboardManagerHelper(this)
        clipboardHelper.startListening()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        voiceHelper = VoiceInputHelper(
            context = this,
            onTextRecognized = { text ->
                currentInputConnection?.commitText(text, 1)
            },
            onListeningStateChanged = { listening ->
                btnVoiceMic?.text = if (listening) "🔴" else "🎙️"
                if (listening) {
                    Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Smart suggestions & personal learning
        learningStore = PersonalLearningStore(this, serviceScope)
        suggestionEngine = SuggestionEngine(learningStore)

        // Read default layout
        currentMode = themeManager.defaultStartupLayout
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        rootView = view

        ivBgImage = view.findViewById(R.id.iv_bg_image)
        vBgOverlay = view.findViewById(R.id.v_bg_overlay)
        llCandidateBar = view.findViewById(R.id.ll_candidate_bar)
        btnSwitchMode = view.findViewById(R.id.btn_switch_mode)
        btnClipboardQuick = view.findViewById(R.id.btn_clipboard_quick)
        btnVoiceMic = view.findViewById(R.id.btn_voice_mic)
        llCandidatesContainer = view.findViewById(R.id.ll_candidates_container)
        llClipboardPanel = view.findViewById(R.id.ll_clipboard_panel)
        llClipboardItems = view.findViewById(R.id.ll_clipboard_items)
        llEmojiPanel = view.findViewById(R.id.ll_emoji_panel)
        llEmojiItems = view.findViewById(R.id.ll_emoji_items)
        llKeyboardRows = view.findViewById(R.id.ll_keyboard_rows)

        val btnCloseClipboard = view.findViewById<TextView>(R.id.btn_close_clipboard)
        btnCloseClipboard?.setOnClickListener {
            closeClipboardPanel()
        }

        val btnCloseEmoji = view.findViewById<TextView>(R.id.btn_close_emoji)
        btnCloseEmoji?.setOnClickListener {
            closeEmojiPanel()
        }

        btnSwitchMode?.setOnClickListener {
            cycleNextLayoutMode()
        }

        btnClipboardQuick?.setOnClickListener {
            toggleClipboardPanel()
        }

        btnVoiceMic?.setOnClickListener {
            val isBengali = currentMode != ThemeManager.LAYOUT_ENGLISH
            voiceHelper?.startListening(isBengali)
        }

        setupEmojiList()
        observeClipboardItems()

        llCandidatesContainer?.let { container ->
            candidateViewHelper = CandidateView(this, container) { word ->
                playKeyFeedback()
                val ic = currentInputConnection
                val isAvro = currentMode == ThemeManager.LAYOUT_AVRO

                // Learn selected word if field is safe
                val isSafe = suggestionEngine.isSafeField(currentInputEditorInfo)
                val lang = if (suggestionEngine.isBengaliScript(word)) "bn" else "en"
                learningStore.learnWord(word, lang, isSafe, themeManager.personalLearningEnabled)

                if (isAvro && phoneticBuffer.isNotEmpty()) {
                    ic?.commitText(word + " ", 1)
                    phoneticBuffer.clear()
                } else if (currentWordBuffer.isNotEmpty()) {
                    ic?.deleteSurroundingText(currentWordBuffer.length, 0)
                    ic?.commitText(word + " ", 1)
                    currentWordBuffer.clear()
                } else {
                    ic?.commitText(word + " ", 1)
                }
                lastAutocorrectOriginal = null
                lastAutocorrectReplacement = null
                if (!isCapsLock && isShifted) {
                    isShifted = false
                    buildKeyboardRows()
                }
                updateCandidates()
            }
        }

        applyThemeAndBuildKeyboard()

        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        phoneticBuffer.clear()
        currentWordBuffer.clear()
        currentCandidates = emptyList()
        lastAutocorrectOriginal = null
        lastAutocorrectReplacement = null
        // English starts capitalized for sentence-start; Bengali layouts start unshifted
        isShifted = (currentMode == ThemeManager.LAYOUT_ENGLISH)
        isCapsLock = false
        isSymbols = false
        isBengaliNumbers = false
        isClipboardOpen = false
        isEmojiOpen = false
        llClipboardPanel?.visibility = View.GONE
        llEmojiPanel?.visibility = View.GONE

        // Refresh theme in case user changed it in Settings
        applyThemeAndBuildKeyboard()
        updateCandidates()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        voiceHelper?.stopListening()
        phoneticBuffer.clear()
        currentWordBuffer.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardHelper.stopListening()
        voiceHelper?.stopListening()
        serviceScope.cancel()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isInputViewShown) {
            val ic = currentInputConnection
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (ic != null) {
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
                        playKeyFeedback()
                        return true
                    }
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (ic != null) {
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
                        playKeyFeedback()
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isInputViewShown) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun cycleNextLayoutMode() {
        playKeyFeedback()
        phoneticBuffer.clear()
        currentWordBuffer.clear()
        val enabledList = themeManager.getOrderedEnabledLayouts()
        val currentIndex = enabledList.indexOf(currentMode)
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % enabledList.size else 0
        currentMode = enabledList[nextIndex]
        isSymbols = false
        closeEmojiPanel()
        closeClipboardPanel()
        applyThemeAndBuildKeyboard()
        updateCandidates()
    }

    private fun cyclePreviousLayoutMode() {
        playKeyFeedback()
        phoneticBuffer.clear()
        currentWordBuffer.clear()
        val enabledList = themeManager.getOrderedEnabledLayouts()
        val currentIndex = enabledList.indexOf(currentMode)
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else enabledList.size - 1
        currentMode = enabledList[prevIndex]
        isSymbols = false
        closeEmojiPanel()
        closeClipboardPanel()
        applyThemeAndBuildKeyboard()
        updateCandidates()
    }

    private fun toggleClipboardPanel() {
        playKeyFeedback()
        if (isClipboardOpen) {
            closeClipboardPanel()
        } else {
            closeEmojiPanel()
            isClipboardOpen = true
            llClipboardPanel?.visibility = View.VISIBLE
        }
    }

    private fun closeClipboardPanel() {
        isClipboardOpen = false
        llClipboardPanel?.visibility = View.GONE
    }

    private fun toggleEmojiPanel() {
        playKeyFeedback()
        if (isEmojiOpen) {
            closeEmojiPanel()
        } else {
            closeClipboardPanel()
            isEmojiOpen = true
            llEmojiPanel?.visibility = View.VISIBLE
        }
    }

    private fun closeEmojiPanel() {
        isEmojiOpen = false
        llEmojiPanel?.visibility = View.GONE
    }

    private var lastKnownRecentClip: String? = null

    private fun observeClipboardItems() {
        serviceScope.launch {
            clipboardHelper.getAllClips().collectLatest { clips ->
                lastKnownRecentClip = clips.firstOrNull()?.text
                populateClipboardPanel(clips)
            }
        }
    }

    private fun pasteLatestClipboardItem() {
        clipboardHelper.checkAndSavePrimaryClip()
        val sysClip = clipboardHelper.getMostRecentClipText()
        val clipText = if (!sysClip.isNullOrBlank()) sysClip else lastKnownRecentClip
        val ic = currentInputConnection
        if (!clipText.isNullOrEmpty()) {
            ic?.commitText(clipText, 1)
            currentWordBuffer.clear()
            updateCandidates()
        } else {
            ic?.commitText("V", 1)
            currentWordBuffer.append("V")
            updateCandidates()
        }
    }

    private fun populateClipboardPanel(clips: List<ClipboardItem>) {
        val container = llClipboardItems ?: return
        container.removeAllViews()

        if (clips.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "কোন ক্লিপ নেই (কপি করলে এখানে জমা হবে)"
                setTextColor(ColorUtils.setAlphaComponent(themeManager.getCurrentTheme().textColor, 180))
                textSize = 13f
                setPadding(16, 16, 16, 16)
            }
            container.addView(emptyTv)
            return
        }

        val theme = themeManager.getCurrentTheme()
        clips.forEach { clip ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(8, 6, 8, 6)
                }
                setPadding(16, 8, 12, 8)
                val cardDrawable = GradientDrawable().apply {
                    cornerRadius = 16f
                    setColor(theme.keyColor)
                    setStroke(2, if (clip.isPinned) theme.accentColor else ColorUtils.setAlphaComponent(theme.textColor, 40))
                }
                background = cardDrawable
            }

            val textTv = TextView(this).apply {
                text = if (clip.text.length > 35) clip.text.take(35) + "..." else clip.text
                setTextColor(theme.textColor)
                textSize = 13f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 8
                }
            }
            card.addView(textTv)

            val pinTv = TextView(this).apply {
                text = if (clip.isPinned) "📌" else "📍"
                textSize = 14f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    playKeyFeedback()
                    clipboardHelper.togglePin(clip)
                }
            }
            card.addView(pinTv)

            card.setOnClickListener {
                playKeyFeedback()
                currentInputConnection?.commitText(clip.text, 1)
                closeClipboardPanel()
            }

            container.addView(card)
        }
    }

    private fun setupEmojiList() {
        val container = llEmojiItems ?: return
        container.removeAllViews()

        val theme = themeManager.getCurrentTheme()
        BengaliLayouts.EMOJIS.forEach { emoji ->
            val btn = TextView(this).apply {
                text = emoji
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(16, 12, 16, 12)
                background = createKeyRippleDrawable(theme.keyColor, theme.keyPressedColor)
                setOnClickListener {
                    playKeyFeedback()
                    currentInputConnection?.commitText(emoji, 1)
                }
            }
            container.addView(btn)
        }
    }

    private fun updateCandidates() {
        if (!themeManager.wordPredictionsEnabled) {
            candidateViewHelper?.clear()
            currentCandidates = emptyList()
            return
        }

        val query = if (currentMode == ThemeManager.LAYOUT_AVRO) {
            phoneticBuffer.toString()
        } else {
            currentWordBuffer.toString()
        }

        if (query.isEmpty()) {
            candidateViewHelper?.clear()
            currentCandidates = emptyList()
            return
        }

        val genId = ++suggestionGenerationId
        val ic = currentInputConnection
        val textBefore = ic?.getTextBeforeCursor(40, 0)?.toString() ?: ""
        val precedingWord = extractPrecedingWord(textBefore, query)
        val editorInfo = currentInputEditorInfo
        val isAvro = (currentMode == ThemeManager.LAYOUT_AVRO)

        serviceScope.launch(Dispatchers.Default) {
            val candidates = suggestionEngine.getSuggestions(
                query = query,
                precedingWord = precedingWord,
                editorInfo = editorInfo,
                suggestionsEnabled = themeManager.wordPredictionsEnabled,
                autocorrectEnabled = themeManager.autocorrectEnabled,
                personalLearningEnabled = themeManager.personalLearningEnabled,
                aggressiveness = themeManager.autocorrectAggressiveness,
                isAvroMode = isAvro
            )
            withContext(Dispatchers.Main) {
                if (genId == suggestionGenerationId) {
                    currentCandidates = candidates
                    val theme = themeManager.getCurrentTheme()
                    val fontScale = themeManager.fontSizeScale.coerceIn(0.80f, 2.50f)
                    candidateViewHelper?.renderSuggestionCandidates(candidates, theme, fontScale)
                }
            }
        }
    }

    private fun extractPrecedingWord(textBeforeCursor: String, currentWord: String): String? {
        val withoutCurrent = if (textBeforeCursor.endsWith(currentWord)) {
            textBeforeCursor.substring(0, textBeforeCursor.length - currentWord.length)
        } else {
            textBeforeCursor
        }
        val trimmed = withoutCurrent.trimEnd()
        if (trimmed.isEmpty()) return null
        val lastSpace = trimmed.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '.', ',', '।', '!', '?'))
        return if (lastSpace >= 0) {
            trimmed.substring(lastSpace + 1).trim()
        } else {
            trimmed
        }
    }

    private fun getDynamicActionIcon(): String {
        val info = currentInputEditorInfo ?: return "↵"
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> "🔍"
            EditorInfo.IME_ACTION_GO -> "➔"
            EditorInfo.IME_ACTION_SEND -> "📤"
            EditorInfo.IME_ACTION_NEXT -> "⏭️"
            EditorInfo.IME_ACTION_DONE -> "✓"
            else -> "↵"
        }
    }

    private fun performDynamicAction() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
        if (!isCapsLock) {
            isShifted = true
            buildKeyboardRows()
        }
    }

    private fun applyThemeAndBuildKeyboard() {
        val theme = themeManager.getCurrentTheme()
        val root = rootView ?: return

        // Dynamic Mode Switcher button icon (🔄) matching reference specification
        btnSwitchMode?.text = "🔄"
        btnSwitchMode?.contentDescription = "Switch Keyboard Layout (Current: $currentMode)"
        btnSwitchMode?.setTextColor(theme.accentColor)
        btnClipboardQuick?.setTextColor(theme.textColor)
        btnVoiceMic?.setTextColor(theme.textColor)

        // Custom Background Gallery Image
        val bgUriStr = themeManager.bgImageUriString
        if (!bgUriStr.isNullOrEmpty()) {
            loadBackgroundImage(bgUriStr, themeManager.bgImageOpacity)
        } else {
            ivBgImage?.visibility = View.GONE
            vBgOverlay?.visibility = View.GONE
            if (theme.isGradient) {
                val gd = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(theme.gradientStartColor, theme.gradientEndColor)
                )
                root.background = gd
            } else {
                root.setBackgroundColor(theme.backgroundColor)
            }
        }

        llCandidateBar?.setBackgroundColor(
            ColorUtils.setAlphaComponent(theme.backgroundColor, 220)
        )

        buildKeyboardRows()
    }

    private fun loadBackgroundImage(uriStr: String, opacity: Float) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriStr)
                contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Downscale for memory efficiency
                    }
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    withContext(Dispatchers.Main) {
                        ivBgImage?.setImageBitmap(bitmap)
                        ivBgImage?.visibility = View.VISIBLE
                        vBgOverlay?.visibility = View.VISIBLE
                        vBgOverlay?.alpha = opacity.coerceIn(0.0f, 0.95f)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ivBgImage?.visibility = View.GONE
                    vBgOverlay?.visibility = View.GONE
                }
            }
        }
    }

    private fun buildKeyboardRows() {
        val rowsContainer = llKeyboardRows ?: return
        rowsContainer.removeAllViews()

        val theme = themeManager.getCurrentTheme()
        val heightScale = themeManager.keyboardHeightScale.coerceIn(0.80f, 2.50f)
        val fontScale = themeManager.fontSizeScale.coerceIn(0.80f, 2.50f)

        val enabledList = themeManager.getOrderedEnabledLayouts()
        if (!enabledList.contains(currentMode)) {
            currentMode = enabledList.firstOrNull() ?: ThemeManager.LAYOUT_JATIYA
        }

        val actionIcon = getDynamicActionIcon()
        val rowDataList: List<List<KeyboardKey>> = when {
            isSymbols -> {
                if (isBengaliNumbers) BengaliLayouts.getBengaliNumberRows()
                else BengaliLayouts.getSymbolRows()
            }
            else -> KeyboardLayoutMapper.getLayoutRows(currentMode, isShifted, isCapsLock, actionIcon)
        }

        val baseRowHeight = (46 * heightScale).toInt()

        // Programmatically adjust the total height of the root keyboard layout view based on user preference
        val candidateBarHeightDp = 44
        val totalHeightDp = candidateBarHeightDp + (baseRowHeight * rowDataList.size) + 8
        val totalHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            totalHeightDp.toFloat(),
            resources.displayMetrics
        ).toInt()

        rootView?.findViewById<View>(R.id.fl_keyboard_wrapper)?.let { wrapper ->
            val lp = wrapper.layoutParams ?: ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                totalHeightPx
            )
            lp.height = totalHeightPx
            wrapper.layoutParams = lp
        }

        rowDataList.forEach { rowKeys ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        baseRowHeight.toFloat(),
                        resources.displayMetrics
                    ).toInt()
                ).apply {
                    setMargins(0, 2, 0, 2)
                }
                gravity = Gravity.CENTER
            }

            rowKeys.forEach { key ->
                val keyView = createKeyView(key, theme, fontScale)
                rowLayout.addView(keyView)
            }

            rowsContainer.addView(rowLayout)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(key: KeyboardKey, theme: KeyboardTheme, fontScale: Float): View {
        val container = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                key.weight
            ).apply {
                val marginH = (2 * resources.displayMetrics.density).toInt()
                val marginV = (2 * resources.displayMetrics.density).toInt()
                setMargins(marginH, marginV, marginH, marginV)
            }
            isClickable = true
            isFocusable = true
        }

        val keyBgColor = when (key.type) {
            KeyType.SHIFT -> if (isShifted || isCapsLock) theme.accentColor else theme.actionKeyColor
            KeyType.ENTER -> theme.actionKeyColor
            KeyType.BACKSPACE, KeyType.SWITCH_MODE, KeyType.SWITCH_SYMBOLS, KeyType.SWITCH_EMOJI ->
                theme.actionKeyColor
            KeyType.SPACE -> theme.keyColor
            else -> theme.keyColor
        }

        val keyTextColor = when {
            key.type == KeyType.SHIFT && (isShifted || isCapsLock) -> theme.backgroundColor
            key.type == KeyType.ENTER -> if (!theme.isDark) Color.WHITE else ColorUtils.setAlphaComponent(theme.textColor, 255)
            else -> theme.textColor
        }

        val strokeColor = if (!theme.isDark && key.type == KeyType.NORMAL) Color.parseColor("#CBD5E1") else 0
        container.background = createKeyRippleDrawable(keyBgColor, theme.keyPressedColor, strokeColor)

        // Main key label
        val mainLabel = TextView(this).apply {
            val labelText = when (key.type) {
                KeyType.SPACE -> when (currentMode) {
                    ThemeManager.LAYOUT_JATIYA -> "◀  জাতীয়  ▶"
                    ThemeManager.LAYOUT_AVRO -> "◀  Avro  ▶"
                    ThemeManager.LAYOUT_BIJOY -> "◀  বিজয়  ▶"
                    ThemeManager.LAYOUT_PRABHAT -> "◀  Prabhat  ▶"
                    else -> "◀  English  ▶"
                }
                KeyType.SHIFT -> if (isCapsLock) "⇪" else "⬆"
                KeyType.ENTER -> getDynamicActionIcon()
                KeyType.NORMAL -> if (isShifted || isCapsLock) {
                    if (key.shiftOutput.isNotEmpty()) key.shiftOutput else key.label
                } else key.label
                else -> key.label
            }
            text = labelText
            setTextColor(keyTextColor)
            textSize = when (key.type) {
                KeyType.SPACE -> (13f * fontScale).coerceIn(11f, 26f)
                KeyType.SHIFT, KeyType.BACKSPACE, KeyType.ENTER, KeyType.SWITCH_SYMBOLS, KeyType.SWITCH_EMOJI ->
                    (15f * fontScale).coerceIn(12f, 28f)
                else -> (19f * fontScale).coerceIn(14f, 36f)
            }
            setTypeface(Typeface.DEFAULT, if (key.type == KeyType.SPACE) Typeface.NORMAL else Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        container.addView(mainLabel)

        // Sublabel / hint label if exists and normal mode
        if (key.hint != null && !isShifted && !isCapsLock && key.type == KeyType.NORMAL) {
            val hintLabel = TextView(this).apply {
                text = key.hint
                setTextColor(theme.sublabelColor)
                textSize = (10f * fontScale).coerceIn(8f, 18f)
                gravity = Gravity.END or Gravity.TOP
                setPadding(0, 4, 8, 0)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(hintLabel)
        }

        // Spacebar Slide Gesture (Language/Layout Switcher) or Standard Touch
        if (key.type == KeyType.SPACE) {
            var startX = 0f
            var startY = 0f
            var hasSwiped = false
            val minSwipeDistance = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                30f,
                resources.displayMetrics
            )

            val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (e1 == null) return false
                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y
                    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > minSwipeDistance) {
                        hasSwiped = true
                        if (diffX > 0) {
                            cycleNextLayoutMode()
                        } else {
                            cyclePreviousLayoutMode()
                        }
                        return true
                    }
                    return false
                }
            })

            container.isLongClickable = true
            container.setOnLongClickListener {
                cycleNextLayoutMode()
                true
            }

            container.setOnTouchListener { v, event ->
                if (gestureDetector.onTouchEvent(event)) {
                    v.isPressed = false
                    return@setOnTouchListener true
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        hasSwiped = false
                        v.isPressed = true
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - startX
                        val dy = event.y - startY
                        if (!hasSwiped && Math.abs(dx) > minSwipeDistance && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                            hasSwiped = true
                            v.isPressed = false
                            if (dx > 0) {
                                cycleNextLayoutMode()
                            } else {
                                cyclePreviousLayoutMode()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.isPressed = false
                        if (!hasSwiped) {
                            playKeyFeedback()
                            handleKeyInput(key)
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        true
                    }
                    else -> false
                }
            }
        } else {
            // Touch handling and Long Press for non-space keys
            val keyHandler = Handler(Looper.getMainLooper())
            var isLongPressed = false
            var startX = 0f
            var startY = 0f
            val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

            val longPressRunnable = Runnable {
                isLongPressed = true
                container.performLongClick()
            }

            // Implement key long-press functionality
            container.isLongClickable = true
            container.setOnLongClickListener {
                playKeyFeedback()
                if (key.type == KeyType.NORMAL) {
                    // English mode: long-pressing 'V' / 'v' fetches and pastes recent clipboard item
                    if (currentMode == ThemeManager.LAYOUT_ENGLISH &&
                        (key.output.equals("v", ignoreCase = true) || key.label.equals("v", ignoreCase = true))) {
                        pasteLatestClipboardItem()
                        return@setOnLongClickListener true
                    }

                    val targetChar = when {
                        !key.hint.isNullOrEmpty() -> key.hint
                        key.shiftOutput.isNotEmpty() && key.shiftOutput != key.output -> key.shiftOutput
                        key.label.length == 1 && key.label[0].isLetter() -> key.label.uppercase()
                        else -> key.output
                    }
                    val ic = currentInputConnection
                    if (currentMode == ThemeManager.LAYOUT_AVRO && isAsciiLetter(targetChar)) {
                        phoneticBuffer.append(targetChar)
                        updateCandidates()
                    } else {
                        ic?.commitText(targetChar, 1)
                        if (currentMode != ThemeManager.LAYOUT_AVRO) {
                            currentWordBuffer.append(targetChar)
                        }
                        updateCandidates()
                    }
                    true
                } else if (key.type == KeyType.SWITCH_MODE) {
                    cycleNextLayoutMode()
                    true
                } else {
                    false
                }
            }

            container.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isLongPressed = false
                        v.isPressed = true
                        startX = event.x
                        startY = event.y
                        if (key.type == KeyType.BACKSPACE) {
                            playKeyFeedback()
                            handleKeyInput(key)
                            startBackspaceRepeat(key)
                        } else {
                            keyHandler.postDelayed(longPressRunnable, 350L)
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = Math.abs(event.x - startX)
                        val dy = Math.abs(event.y - startY)
                        if (dx > touchSlop || dy > touchSlop) {
                            keyHandler.removeCallbacks(longPressRunnable)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.isPressed = false
                        keyHandler.removeCallbacks(longPressRunnable)
                        if (key.type == KeyType.BACKSPACE) {
                            stopBackspaceRepeat()
                        } else if (!isLongPressed) {
                            playKeyFeedback()
                            handleKeyInput(key)
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        keyHandler.removeCallbacks(longPressRunnable)
                        if (key.type == KeyType.BACKSPACE) {
                            stopBackspaceRepeat()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        return container
    }

    private fun handleKeyInput(key: KeyboardKey) {
        val ic = currentInputConnection ?: return

        when (key.type) {
            KeyType.NORMAL -> {
                val output = if (isShifted || isCapsLock) {
                    if (key.shiftOutput.isNotEmpty()) key.shiftOutput else key.output
                } else {
                    key.output
                }
                lastAutocorrectOriginal = null
                lastAutocorrectReplacement = null
                if (currentMode == ThemeManager.LAYOUT_AVRO && isAsciiLetter(output)) {
                    phoneticBuffer.append(output)
                    updateCandidates()
                } else {
                    val isBengaliLayout = (currentMode == ThemeManager.LAYOUT_JATIYA ||
                                           currentMode == ThemeManager.LAYOUT_BIJOY ||
                                           currentMode == ThemeManager.LAYOUT_PRABHAT)

                    if (isBengaliLayout) {
                        val prevChar = currentWordBuffer.lastOrNull()?.toString()
                            ?: ic.getTextBeforeCursor(1, 0)?.toString()

                        val compResult = com.example.ime.BengaliCompositionEngine.processInput(
                            incoming = output,
                            prevChar = prevChar,
                            isBijoy = (currentMode == ThemeManager.LAYOUT_BIJOY)
                        )

                        when (compResult) {
                            is com.example.ime.CompositionResult.Replace -> {
                                ic.deleteSurroundingText(compResult.charsToDelete, 0)
                                ic.commitText(compResult.replacement, 1)
                                if (currentWordBuffer.length >= compResult.charsToDelete) {
                                    currentWordBuffer.delete(
                                        currentWordBuffer.length - compResult.charsToDelete,
                                        currentWordBuffer.length
                                    )
                                }
                                currentWordBuffer.append(compResult.replacement)
                            }
                            is com.example.ime.CompositionResult.Commit -> {
                                ic.commitText(compResult.text, 1)
                                currentWordBuffer.append(compResult.text)
                            }
                        }
                    } else {
                        ic.commitText(output, 1)
                        currentWordBuffer.append(output)
                    }
                    updateCandidates()
                }

                if (isShifted && !isCapsLock) {
                    isShifted = false
                    buildKeyboardRows()
                }
            }
            KeyType.SHIFT -> {
                if (isSymbols) {
                    isBengaliNumbers = !isBengaliNumbers
                } else {
                    val now = System.currentTimeMillis()
                    if (isCapsLock) {
                        isCapsLock = false
                        isShifted = false
                    } else if (now - lastShiftTapTime < DOUBLE_TAP_TIMEOUT) {
                        isCapsLock = true
                        isShifted = true
                    } else {
                        isShifted = !isShifted
                    }
                    lastShiftTapTime = now
                }
                buildKeyboardRows()
            }
            KeyType.BACKSPACE -> {
                // Revert previous autocorrection if user presses backspace immediately
                if (lastAutocorrectOriginal != null && lastAutocorrectReplacement != null) {
                    val original = lastAutocorrectOriginal!!
                    val replacement = lastAutocorrectReplacement!!
                    ic.deleteSurroundingText(replacement.length + 1, 0)
                    ic.commitText(original, 1)
                    currentWordBuffer.clear()
                    currentWordBuffer.append(original)
                    lastAutocorrectOriginal = null
                    lastAutocorrectReplacement = null
                    updateCandidates()
                    return
                }

                if (phoneticBuffer.isNotEmpty()) {
                    phoneticBuffer.deleteCharAt(phoneticBuffer.length - 1)
                    updateCandidates()
                } else {
                    val textBefore = ic.getTextBeforeCursor(4, 0)?.toString() ?: ""
                    val deleteLen = com.example.ime.BengaliCompositionHelper.getDeletionLength(textBefore)
                    ic.deleteSurroundingText(deleteLen, 0)
                    if (currentWordBuffer.isNotEmpty()) {
                        val bufferDelete = minOf(deleteLen, currentWordBuffer.length)
                        currentWordBuffer.delete(currentWordBuffer.length - bufferDelete, currentWordBuffer.length)
                    }
                    updateCandidates()
                }
            }
            KeyType.SPACE -> {
                val isAvro = (currentMode == ThemeManager.LAYOUT_AVRO)
                val topCandidate = currentCandidates.firstOrNull()

                if (themeManager.autocorrectEnabled && topCandidate != null && topCandidate.isAutocorrect) {
                    val rawWord = if (isAvro) phoneticBuffer.toString() else currentWordBuffer.toString()
                    val replacement = topCandidate.text

                    if (isAvro && phoneticBuffer.isNotEmpty()) {
                        ic.commitText(replacement + " ", 1)
                        phoneticBuffer.clear()
                    } else if (currentWordBuffer.isNotEmpty()) {
                        ic.deleteSurroundingText(currentWordBuffer.length, 0)
                        ic.commitText(replacement + " ", 1)
                        currentWordBuffer.clear()
                    } else {
                        ic.commitText(replacement + " ", 1)
                    }

                    val isSafe = suggestionEngine.isSafeField(currentInputEditorInfo)
                    val lang = if (suggestionEngine.isBengaliScript(replacement)) "bn" else "en"
                    learningStore.learnWord(replacement, lang, isSafe, themeManager.personalLearningEnabled)

                    lastAutocorrectOriginal = rawWord
                    lastAutocorrectReplacement = replacement
                } else {
                    if (isAvro && phoneticBuffer.isNotEmpty()) {
                        val transliterated = BengaliEngine.phoneticTransliterate(phoneticBuffer.toString())
                        ic.commitText(transliterated + " ", 1)
                        val isSafe = suggestionEngine.isSafeField(currentInputEditorInfo)
                        learningStore.learnWord(transliterated, "bn", isSafe, themeManager.personalLearningEnabled)
                        phoneticBuffer.clear()
                    } else {
                        if (currentWordBuffer.isNotEmpty()) {
                            val typed = currentWordBuffer.toString()
                            val isSafe = suggestionEngine.isSafeField(currentInputEditorInfo)
                            val lang = if (suggestionEngine.isBengaliScript(typed)) "bn" else "en"
                            learningStore.learnWord(typed, lang, isSafe, themeManager.personalLearningEnabled)
                        }
                        ic.commitText(" ", 1)
                    }
                    currentWordBuffer.clear()
                    lastAutocorrectOriginal = null
                    lastAutocorrectReplacement = null
                }
                updateCandidates()

                val textBefore = ic.getTextBeforeCursor(4, 0)?.toString() ?: ""
                val trimmed = textBefore.trimEnd()
                if (trimmed.endsWith(".") || trimmed.endsWith("।") || trimmed.endsWith("?") || trimmed.endsWith("!")) {
                    if (!isCapsLock) {
                        isShifted = true
                        buildKeyboardRows()
                    }
                }
            }
            KeyType.ENTER -> {
                val isSafe = suggestionEngine.isSafeField(currentInputEditorInfo)
                if (phoneticBuffer.isNotEmpty()) {
                    val transliterated = BengaliEngine.phoneticTransliterate(phoneticBuffer.toString())
                    ic.commitText(transliterated, 1)
                    learningStore.learnWord(transliterated, "bn", isSafe, themeManager.personalLearningEnabled)
                    phoneticBuffer.clear()
                } else if (currentWordBuffer.isNotEmpty()) {
                    val typed = currentWordBuffer.toString()
                    val lang = if (suggestionEngine.isBengaliScript(typed)) "bn" else "en"
                    learningStore.learnWord(typed, lang, isSafe, themeManager.personalLearningEnabled)
                }
                currentWordBuffer.clear()
                lastAutocorrectOriginal = null
                lastAutocorrectReplacement = null
                performDynamicAction()
                updateCandidates()
            }
            KeyType.SWITCH_SYMBOLS -> {
                isSymbols = !isSymbols
                isBengaliNumbers = false
                buildKeyboardRows()
            }
            KeyType.SWITCH_EMOJI -> {
                toggleEmojiPanel()
            }
            KeyType.SWITCH_MODE -> {
                cycleNextLayoutMode()
            }
            KeyType.CLIPBOARD -> {
                toggleClipboardPanel()
            }
            KeyType.VOICE -> {
                val isBengali = currentMode != ThemeManager.LAYOUT_ENGLISH
                voiceHelper?.startListening(isBengali)
            }
        }
    }

    private fun isAsciiLetter(str: String): Boolean {
        return str.length == 1 && str[0] in 'a'..'z' || str[0] in 'A'..'Z'
    }

    private fun startBackspaceRepeat(key: KeyboardKey) {
        stopBackspaceRepeat()
        backspaceRunnable = object : Runnable {
            override fun run() {
                playKeyFeedback()
                handleKeyInput(key)
                repeatHandler.postDelayed(this, 60)
            }
        }
        repeatHandler.postDelayed(backspaceRunnable!!, 400)
    }

    private fun stopBackspaceRepeat() {
        backspaceRunnable?.let {
            repeatHandler.removeCallbacks(it)
            backspaceRunnable = null
        }
    }

    private fun playKeyFeedback() {
        if (themeManager.hapticFeedbackEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(22)
                }
            } catch (_: Exception) {}
        }

        if (themeManager.keySoundEnabled) {
            try {
                audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
            } catch (_: Exception) {}
        }
    }

    private fun createKeyRippleDrawable(normalColor: Int, pressedColor: Int, strokeColor: Int = 0): RippleDrawable {
        val normalDrawable = GradientDrawable().apply {
            cornerRadius = 14f
            setColor(normalColor)
            if (strokeColor != 0) {
                setStroke(1, strokeColor)
            }
        }
        val maskDrawable = GradientDrawable().apply {
            cornerRadius = 14f
            setColor(android.graphics.Color.WHITE)
        }
        return RippleDrawable(
            ColorStateList.valueOf(pressedColor),
            normalDrawable,
            maskDrawable
        )
    }
}
