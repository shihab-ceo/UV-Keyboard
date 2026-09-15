package com.example.translator

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

/**
 * Compact, crash-proof in-keyboard Live Translator Toolbar View.
 *
 * Design & Stability:
 * 1. Threading: UI operations remain strictly on the Main thread, while network translations
 *    are dispatched asynchronously to Dispatchers.IO via [TranslatorEngine].
 * 2. Non-Focusable Architecture: Uses non-focusable UI elements for the input preview,
 *    eliminating IME focus recursion loops and NullPointerExceptions when the IME window attaches.
 * 3. Explicit Mode Guard: Standard keyboard typing never triggers translation logic unless
 *    [isTranslateModeActive] is explicitly toggled ON by the user.
 * 4. Initialization Guard: Dropdown spinners do not fire unrequested translations during layout.
 */
class TranslatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val engine = TranslatorEngine()

    var isTranslateModeActive: Boolean = false
        set(value) {
            field = value
            if (!value) {
                clearInput()
            }
        }

    private var currentSourceLang = TranslatorEngine.DEFAULT_SOURCE_LANG
    private var currentTargetLang = TranslatorEngine.DEFAULT_TARGET_LANG
    private var currentInputText: String = ""
    private var latestTranslatedText: String = ""

    // Callbacks
    var onTranslationReady: ((originalText: String, translatedText: String) -> Unit)? = null
    var onCommitRequested: ((translatedText: String) -> Unit)? = null
    var onCloseClicked: (() -> Unit)? = null

    // UI Elements
    private lateinit var sourceSpinner: Spinner
    private lateinit var targetSpinner: Spinner
    private lateinit var btnSwap: TextView
    private lateinit var btnClose: TextView
    private lateinit var tvInputDisplay: TextView
    private lateinit var btnClear: TextView
    private lateinit var btnCommit: TextView
    private lateinit var tvPreview: TextView
    private lateinit var tvStatus: TextView

    private val languages = TranslatorEngine.SUPPORTED_LANGUAGES

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setBackgroundColor(Color.parseColor("#182234"))
        setPadding(dp(6), dp(2), dp(6), dp(2))

        buildUi()
    }

    private fun buildUi() {
        removeAllViews()

        // 1. Language selector & controls row (compact 26dp)
        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(26))
        }

        // Translation Badge / Icon
        val tvIcon = TextView(context).apply {
            text = "文A"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(dp(2), 0, dp(4), 0)
        }
        topRow.addView(tvIcon)

        // Source Language Dropdown
        sourceSpinner = createLanguageSpinner(currentSourceLang) { selected ->
            if (currentSourceLang != selected.code) {
                currentSourceLang = selected.code
                updateInputHint()
                if (isTranslateModeActive && currentInputText.isNotEmpty()) {
                    triggerTranslation()
                }
            }
        }
        val sourceSpinnerParams = LayoutParams(0, dp(24), 1f)
        sourceSpinner.layoutParams = sourceSpinnerParams
        topRow.addView(sourceSpinner)

        // Swap Languages Button (⇄)
        btnSwap = TextView(context).apply {
            text = "⇄"
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(28), dp(24)).apply {
                marginEnd = dp(2)
                marginStart = dp(2)
            }
            background = createRippleDrawable(Color.parseColor("#24334B"))
            contentDescription = "Swap Languages"
            setOnClickListener {
                swapLanguages()
            }
        }
        topRow.addView(btnSwap)

        // Target Language Dropdown
        targetSpinner = createLanguageSpinner(currentTargetLang) { selected ->
            if (currentTargetLang != selected.code) {
                currentTargetLang = selected.code
                if (isTranslateModeActive && currentInputText.isNotEmpty()) {
                    triggerTranslation()
                }
            }
        }
        val targetSpinnerParams = LayoutParams(0, dp(24), 1f)
        targetSpinner.layoutParams = targetSpinnerParams
        topRow.addView(targetSpinner)

        // Close Translator Button (✖)
        btnClose = TextView(context).apply {
            text = "✖"
            textSize = 13f
            setTextColor(Color.parseColor("#EF4444"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(26), dp(24)).apply {
                marginStart = dp(2)
            }
            background = createRippleDrawable(Color.parseColor("#2A1E2B"))
            contentDescription = "Close Translator"
            setOnClickListener {
                onCloseClicked?.invoke()
            }
        }
        topRow.addView(btnClose)

        addView(topRow)

        // 2. Compact Input Display Row (26dp)
        val inputContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(26)).apply {
                topMargin = dp(2)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A"))
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), Color.parseColor("#334155"))
            }
            setPadding(dp(6), 0, dp(4), 0)
        }

        tvInputDisplay = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 11.5f
            maxLines = 1
            isFocusable = false
            isFocusableInTouchMode = false
            contentDescription = "Translation Input Text"
            updateInputHint()
        }
        inputContainer.addView(tvInputDisplay)

        // Clear input button (✕)
        btnClear = TextView(context).apply {
            text = "✕"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(26), dp(26))
            visibility = View.GONE
            contentDescription = "Clear Input"
            setOnClickListener {
                clearInput()
            }
        }
        inputContainer.addView(btnClear)

        // Commit translation button (✓)
        btnCommit = TextView(context).apply {
            text = "✓"
            textSize = 14f
            setTextColor(Color.parseColor("#10B981"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(30), dp(26)).apply {
                marginStart = dp(2)
            }
            background = createRippleDrawable(Color.parseColor("#1E3A33"))
            visibility = View.GONE
            contentDescription = "Commit Translation"
            setOnClickListener {
                commitCurrentTranslation()
            }
        }
        inputContainer.addView(btnCommit)

        addView(inputContainer)

        // 3. Compact Live Translation Preview row (18dp)
        val previewRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(18)).apply {
                topMargin = dp(1)
            }
        }

        tvStatus = TextView(context).apply {
            text = "Live"
            textSize = 9f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(4)
            }
        }
        previewRow.addView(tvStatus)

        tvPreview = TextView(context).apply {
            text = "Type on keyboard below to live translate"
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            maxLines = 1
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        previewRow.addView(tvPreview)

        addView(previewRow)
    }

    private fun createLanguageSpinner(
        defaultCode: String,
        onSelected: (Language) -> Unit
    ): Spinner {
        return Spinner(context, Spinner.MODE_DROPDOWN).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), Color.parseColor("#334155"))
            }

            val adapter = object : ArrayAdapter<Language>(
                context,
                0,
                languages
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val tv = (convertView as? TextView) ?: TextView(context).apply {
                        gravity = Gravity.CENTER
                        setPadding(dp(4), dp(2), dp(4), dp(2))
                    }
                    tv.setTextColor(Color.parseColor("#F8FAFC"))
                    tv.textSize = 12f
                    tv.setTypeface(null, Typeface.BOLD)
                    val lang = getItem(position)
                    tv.text = "${lang?.nativeName ?: ""} (${lang?.code?.uppercase() ?: ""})"
                    return tv
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val tv = (convertView as? TextView) ?: TextView(context).apply {
                        setPadding(dp(12), dp(10), dp(12), dp(10))
                    }
                    tv.setTextColor(Color.parseColor("#F8FAFC"))
                    tv.setBackgroundColor(Color.parseColor("#1E293B"))
                    tv.textSize = 13f
                    val lang = getItem(position)
                    tv.text = "${lang?.nativeName ?: ""} - ${lang?.name ?: ""}"
                    return tv
                }
            }

            this.adapter = adapter
            val defaultIndex = languages.indexOfFirst { it.code.equals(defaultCode, ignoreCase = true) }
            if (defaultIndex >= 0) {
                setSelection(defaultIndex, false)
            }

            var initialLayoutPassed = false
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!initialLayoutPassed) {
                        initialLayoutPassed = true
                        return
                    }
                    val lang = languages.getOrNull(position) ?: return
                    onSelected(lang)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun updateInputHint() {
        if (!::tvInputDisplay.isInitialized) return
        val srcLang = engine.getLanguageByCode(currentSourceLang)
        if (currentInputText.isEmpty()) {
            tvInputDisplay.text = "Type in ${srcLang.name} (${srcLang.nativeName})..."
            tvInputDisplay.setTextColor(Color.parseColor("#64748B"))
        } else {
            tvInputDisplay.text = currentInputText
            tvInputDisplay.setTextColor(Color.parseColor("#F8FAFC"))
        }
    }

    /**
     * Executes real-time translation with 300ms debounce.
     * Guaranteed to execute network on Dispatchers.IO and invoke UI on Main thread.
     */
    fun triggerTranslation() {
        if (!isTranslateModeActive) return

        val text = currentInputText.trim()
        if (text.isEmpty()) {
            latestTranslatedText = ""
            tvStatus.text = "Live"
            tvStatus.setTextColor(Color.parseColor("#10B981"))
            tvPreview.text = "Type in ${engine.getLanguageByCode(currentSourceLang).name} to translate"
            onTranslationReady?.invoke("", "")
            return
        }

        tvStatus.text = "..."
        tvStatus.setTextColor(Color.parseColor("#F59E0B"))

        engine.translateDebounced(
            text = text,
            sourceLang = currentSourceLang,
            targetLang = currentTargetLang
        ) { translated ->
            latestTranslatedText = translated
            tvStatus.text = "✓"
            tvStatus.setTextColor(Color.parseColor("#10B981"))
            tvPreview.text = translated
            if (isTranslateModeActive) {
                onTranslationReady?.invoke(text, translated)
            }
        }
    }

    /**
     * Swaps source and target languages.
     */
    fun swapLanguages() {
        val oldSource = currentSourceLang
        val oldTarget = currentTargetLang

        currentSourceLang = oldTarget
        currentTargetLang = oldSource

        val srcIdx = languages.indexOfFirst { it.code.equals(currentSourceLang, ignoreCase = true) }
        val tgtIdx = languages.indexOfFirst { it.code.equals(currentTargetLang, ignoreCase = true) }

        if (srcIdx >= 0) sourceSpinner.setSelection(srcIdx, false)
        if (tgtIdx >= 0) targetSpinner.setSelection(tgtIdx, false)

        updateInputHint()

        if (latestTranslatedText.isNotEmpty()) {
            currentInputText = latestTranslatedText
            updateInputHint()
        }

        if (isTranslateModeActive && currentInputText.isNotEmpty()) {
            triggerTranslation()
        }
    }

    /**
     * Commits current translated text and resets the input buffer.
     */
    fun commitCurrentTranslation() {
        val toCommit = if (latestTranslatedText.isNotEmpty()) latestTranslatedText else currentInputText
        if (toCommit.isNotEmpty()) {
            onCommitRequested?.invoke(toCommit)
            clearInput()
        }
    }

    fun appendInputText(charOrWord: String) {
        currentInputText += charOrWord
        updateInputView()
        triggerTranslation()
    }

    fun deleteLastChar() {
        if (currentInputText.isNotEmpty()) {
            currentInputText = currentInputText.substring(0, currentInputText.length - 1)
            updateInputView()
            triggerTranslation()
        }
    }

    fun setInputText(text: String) {
        currentInputText = text
        updateInputView()
        triggerTranslation()
    }

    private fun updateInputView() {
        updateInputHint()
        btnClear.visibility = if (currentInputText.isNotEmpty()) View.VISIBLE else View.GONE
        btnCommit.visibility = if (currentInputText.isNotEmpty()) View.VISIBLE else View.GONE
    }

    fun clearInput() {
        currentInputText = ""
        latestTranslatedText = ""
        updateInputHint()
        if (::btnClear.isInitialized) btnClear.visibility = View.GONE
        if (::btnCommit.isInitialized) btnCommit.visibility = View.GONE
        if (::tvStatus.isInitialized) {
            tvStatus.text = "Live"
            tvStatus.setTextColor(Color.parseColor("#10B981"))
        }
        if (::tvPreview.isInitialized) {
            tvPreview.text = "Type in ${engine.getLanguageByCode(currentSourceLang).name} to translate"
        }
        engine.cancelInFlight()
        if (isTranslateModeActive) {
            onTranslationReady?.invoke("", "")
        }
    }

    fun getInputText(): String = currentInputText

    fun getTranslatedText(): String = latestTranslatedText

    fun getSourceLanguage(): String = currentSourceLang

    fun getTargetLanguage(): String = currentTargetLang

    fun setSourceLanguage(code: String) {
        val idx = languages.indexOfFirst { it.code.equals(code, ignoreCase = true) }
        if (idx >= 0) {
            currentSourceLang = code
            if (::sourceSpinner.isInitialized) {
                sourceSpinner.setSelection(idx, false)
            }
            updateInputHint()
        }
    }

    fun setTargetLanguage(code: String) {
        val idx = languages.indexOfFirst { it.code.equals(code, ignoreCase = true) }
        if (idx >= 0) {
            currentTargetLang = code
            if (::targetSpinner.isInitialized) {
                targetSpinner.setSelection(idx, false)
            }
        }
    }

    fun onDestroy() {
        runCatching {
            engine.destroy()
        }
    }

    private fun createRippleDrawable(pressedColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(pressedColor)
            cornerRadius = dp(6).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
