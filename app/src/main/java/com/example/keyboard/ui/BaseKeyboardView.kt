package com.example.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.example.CustomThemeModel
import com.example.CustomThemeRepository
import com.example.KeyboardTheme
import com.example.ThemeManager
import com.example.ime.KeyboardKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Abstract/Base Keyboard View encapsulating all common UI, Theming, Dimensions,
 * Feedback, Gesture, and Rendering pipelines for all keyboard layouts.
 *
 * Future layouts (Jatiya, Bijoy, Avro, Prabhat, English) automatically inherit:
 * 1. Theme & Background Pipeline: Preset themes, custom solid colors, gradients, and gallery images with opacity.
 * 2. Text & Font Scale Pipeline: Dynamic text colors, font sizing scale, key background drawables, and height scaling.
 * 3. Feedback & Input Pipeline: Audio effects, haptic vibration, continuous backspace repeat, and spacebar gestures.
 * 4. Canvas Utilities: Dotted circle (U+25CC) stripping and standardized key/label drawing.
 */
abstract class BaseKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- Services & Managers ---
    protected val themeManager = ThemeManager(context)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val prefs: SharedPreferences =
        context.getSharedPreferences("uv_keyboard_theme_prefs", Context.MODE_PRIVATE)
    private val customPrefs: SharedPreferences =
        context.getSharedPreferences(CustomThemeRepository.PREFS_NAME, Context.MODE_PRIVATE)

    private val viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // --- Key Callbacks ---
    var onKeyCommitListener: ((text: String) -> Unit)? = null
    var onBackspaceListener: (() -> Unit)? = null
    var onEnterListener: (() -> Unit)? = null
    var onSpaceListener: (() -> Unit)? = null
    var onShiftToggleListener: ((isShifted: Boolean) -> Unit)? = null
    var onSpacebarSwipeNextListener: (() -> Unit)? = null
    var onSpacebarSwipePrevListener: (() -> Unit)? = null
    var onSymbolsToggleListener: (() -> Unit)? = null
    var onEmojiToggleListener: (() -> Unit)? = null
    var onSettingsHintListener: (() -> Unit)? = null

    // --- Keyboard States ---
    var isShifted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var isCapsLock: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var enterActionIcon: String = "↵"
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Compact mode adjusts vertical key paddings and allocates a safe bottom inset
     * when an extension header (e.g. Translator) is active, guaranteeing that the
     * bottom row keys (Spacebar / Enter) remain 100% visible and unclipped.
     */
    var isCompactMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (width > 0 && height > 0) {
                    calculateKeyBounds(width, height)
                    updatePaintSizes(height)
                    invalidate()
                }
            }
        }

    // --- Text & Font Scale Pipeline ---
    var fontScale: Float = themeManager.fontSizeScale.coerceIn(0.80f, 2.50f)
        set(value) {
            field = value
            updatePaintSizes(height)
            invalidate()
        }

    var heightScale: Float = themeManager.keyboardHeightScale.coerceIn(0.80f, 2.50f)
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var dynamicTextColor: Int? = null
        set(value) {
            field = value
            updateThemePaints()
            invalidate()
        }

    val effectiveTextColor: Int
        get() = dynamicTextColor ?: keyTextColor

    // --- Theme & Colors ---
    protected var currentTheme: KeyboardTheme = themeManager.getCurrentTheme()
    protected var keyboardBackgroundColor: Int = currentTheme.backgroundColor
    protected var keyBackgroundColor: Int = currentTheme.keyColor
    protected var keyPressedColor: Int = currentTheme.keyPressedColor
    protected var actionKeyColor: Int = currentTheme.actionKeyColor
    protected var keyTextColor: Int = currentTheme.textColor
    protected var keyAccentColor: Int = currentTheme.accentColor
    protected var keySublabelColor: Int = currentTheme.sublabelColor
    protected var isDarkTheme: Boolean = currentTheme.isDark
    protected var keyCornerRadius: Float = 14f

    // Background Image
    protected var currentBgBitmap: Bitmap? = null
    protected var currentBgOpacity: Float = themeManager.bgImageOpacity

    // --- Paints ---
    protected val keyboardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val keyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val keyPressedBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val specialKeyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val shiftKeyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val keyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * context.resources.displayMetrics.density
    }

    protected val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    protected val spacebarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    protected val shiftIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    protected val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    protected val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    protected val bgOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // --- Key Types & Item Models ---
    sealed class KeyType {
        data class Character(val payload: Any) : KeyType()
        object Shift : KeyType()
        object Backspace : KeyType()
        object Symbols : KeyType()
        object Emoji : KeyType()
        object Comma : KeyType()
        object Space : KeyType()
        object Dari : KeyType()
        object Enter : KeyType()
    }

    open class BaseKeyItem(
        val rect: Rect,
        val rectF: RectF,
        val type: KeyType
    )

    protected val keyItems = mutableListOf<BaseKeyItem>()

    // --- Touch, Gesture & Feedback Pipeline ---
    protected val uiHandler = Handler(Looper.getMainLooper())
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val minSwipeDistance = 30f * context.resources.displayMetrics.density

    protected var activePressedKey: BaseKeyItem? = null
    protected var isLongPressTriggered = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isSpaceSwipeHandled = false

    private val longPressRunnable = Runnable {
        activePressedKey?.let { item ->
            isLongPressTriggered = true
            handleKeyLongPress(item)
            invalidate()
        }
    }

    // Continuous Repeatable Backspace Runnable & Logic
    private val backspaceRunnable = object : Runnable {
        override fun run() {
            playKeyFeedback()
            onBackspaceListener?.invoke()
            uiHandler.postDelayed(this, BACKSPACE_REPEAT_INTERVAL_MS)
        }
    }

    // Preference Observer to automatically sync settings without layout-specific updates
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                ThemeManager.KEY_THEME_ID,
                ThemeManager.KEY_CUSTOM_BG_COLOR,
                ThemeManager.KEY_USE_CUSTOM_COLOR,
                ThemeManager.KEY_BG_IMAGE_URI,
                ThemeManager.KEY_BG_IMAGE_OPACITY,
                ThemeManager.KEY_ACTIVE_CUSTOM_THEME_ID -> {
                    updateFromThemeManager()
                }
                ThemeManager.KEY_FONT_SIZE_SCALE -> {
                    fontScale = themeManager.fontSizeScale.coerceIn(0.80f, 2.50f)
                }
                ThemeManager.KEY_KEYBOARD_HEIGHT_SCALE -> {
                    heightScale = themeManager.keyboardHeightScale.coerceIn(0.80f, 2.50f)
                }
            }
        }

    private val customPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            updateFromThemeManager()
        }

    init {
        applyTheme(themeManager.getCurrentTheme())
        loadBackgroundImageIfConfigured()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        customPrefs.registerOnSharedPreferenceChangeListener(customPreferenceChangeListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        customPrefs.unregisterOnSharedPreferenceChangeListener(customPreferenceChangeListener)
        uiHandler.removeCallbacks(longPressRunnable)
        stopBackspaceRepeat()
        viewScope.cancel()
    }

    /**
     * Updates theme settings directly from ThemeManager.
     */
    fun updateFromThemeManager() {
        val theme = themeManager.getCurrentTheme()
        applyTheme(theme)
        loadBackgroundImageIfConfigured()
    }

    /**
     * Dynamically applies active theme colors.
     */
    open fun applyTheme(theme: KeyboardTheme) {
        this.currentTheme = theme
        this.keyboardBackgroundColor = theme.backgroundColor
        this.keyBackgroundColor = theme.keyColor
        this.keyPressedColor = theme.keyPressRippleColor
        this.actionKeyColor = theme.actionKeyColor
        this.keyTextColor = theme.textColor
        this.keyAccentColor = theme.accentColor
        this.keySublabelColor = theme.sublabelColor
        this.isDarkTheme = theme.isDark
        val density = context.resources.displayMetrics.density
        this.keyCornerRadius = (theme.keyCornerRadius * density).coerceIn(0f, 24f * density)

        updateThemePaints()
        invalidate()
    }

    /**
     * Overload for applying custom theme model directly.
     */
    open fun applyTheme(customTheme: CustomThemeModel) {
        applyTheme(customTheme.toKeyboardTheme())
    }

    protected open fun updateThemePaints() {
        keyboardBgPaint.color = keyboardBackgroundColor

        // Apply alpha transparency level to all normal key backgrounds
        val keyAlpha = (currentTheme.keyBackgroundOpacity.coerceIn(0.10f, 1.0f) * 255).toInt()
        keyBackgroundPaint.color = keyBackgroundColor
        keyBackgroundPaint.alpha = keyAlpha

        keyPressedBackgroundPaint.color = keyPressedColor
        specialKeyBackgroundPaint.color = actionKeyColor
        
        // Dynamically set Shift key background to theme.accentColor across ALL layouts
        shiftKeyBackgroundPaint.color = keyAccentColor

        val txtColor = effectiveTextColor
        primaryPaint.color = txtColor
        spacebarPaint.color = txtColor

        // Ensure Shift key arrow icon has optimal contrast against the dynamic accent color
        val lum = (Color.red(keyAccentColor) * 0.299 +
                Color.green(keyAccentColor) * 0.587 +
                Color.blue(keyAccentColor) * 0.114) / 255.0
        shiftIconPaint.color = if (lum < 0.55) Color.WHITE else Color.parseColor("#0F172A")

        specialKeyPaint.color = txtColor
        hintPaint.color = keySublabelColor

        keyStrokePaint.color = if (!isDarkTheme) Color.parseColor("#CBD5E1") else Color.TRANSPARENT
    }

    protected fun loadBackgroundImageIfConfigured() {
        val uriStr = themeManager.bgImageUriString
        this.currentBgOpacity = themeManager.bgImageOpacity

        if (uriStr.isNullOrEmpty()) {
            currentBgBitmap = null
            invalidate()
            return
        }

        viewScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Memory-safe downscale
                    }
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    withContext(Dispatchers.Main) {
                        currentBgBitmap = bitmap
                        invalidate()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    currentBgBitmap = null
                    invalidate()
                }
            }
        }
    }

    // --- Dimension and Font Calculation Pipeline ---
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val targetHeight = if (heightMode == MeasureSpec.EXACTLY || (heightMode == MeasureSpec.AT_MOST && heightSize > 0)) {
            heightSize
        } else {
            val density = context.resources.displayMetrics.density
            (230 * density * heightScale.coerceIn(0.85f, 1.4f)).toInt()
        }
        setMeasuredDimension(width, targetHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateKeyBounds(w, h)
        updatePaintSizes(h)
    }

    /**
     * Calculates font sizes across all key paints based on total height and font scale.
     */
    protected open fun updatePaintSizes(totalHeight: Int) {
        if (totalHeight <= 0) return
        val density = context.resources.displayMetrics.density
        val effectiveH = if (isCompactMode) (totalHeight - (8f * density)).coerceAtLeast(100f) else totalHeight.toFloat()
        val rowHeight = effectiveH / 4f
        val scaleMultiplier = (fontScale / 1.4f).coerceIn(0.7f, 1.8f)

        val charTextSize = (rowHeight * 0.42f * scaleMultiplier).coerceIn(18f, 60f)
        val specialTextSize = (rowHeight * 0.35f * scaleMultiplier).coerceIn(16f, 50f)
        val spacebarTextSize = (rowHeight * 0.29f * scaleMultiplier).coerceIn(13f, 40f)
        val hintTextSize = (rowHeight * 0.22f * scaleMultiplier).coerceIn(10f, 30f)

        primaryPaint.textSize = charTextSize
        shiftIconPaint.textSize = (specialTextSize * 1.15f).coerceIn(22f, 56f)
        specialKeyPaint.textSize = specialTextSize
        spacebarPaint.textSize = spacebarTextSize
        hintPaint.textSize = hintTextSize
    }

    // --- Feedback Pipeline ---
    fun playKeyFeedback() {
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

    protected fun startBackspaceRepeat() {
        stopBackspaceRepeat()
        uiHandler.postDelayed(backspaceRunnable, BACKSPACE_INITIAL_DELAY_MS)
    }

    protected fun stopBackspaceRepeat() {
        uiHandler.removeCallbacks(backspaceRunnable)
    }

    // --- Canvas Utilities & Rendering Pipeline ---
    protected val labelTextBounds = Rect()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawKeyboardBackground(canvas)

        for (item in keyItems) {
            val isPressed = (item == activePressedKey && !isSpaceSwipeHandled)
            drawKeyBackground(canvas, item, isPressed)

            when (val type = item.type) {
                is KeyType.Character -> {
                    drawCharacterKey(canvas, item, type.payload, isShifted)
                }
                is KeyType.Shift -> {
                    drawShiftKey(canvas, item)
                }
                is KeyType.Backspace -> {
                    drawCenteredLabel(canvas, item.rect, "⌫", specialKeyPaint)
                }
                is KeyType.Symbols -> {
                    drawCenteredLabel(canvas, item.rect, "?123", specialKeyPaint)
                }
                is KeyType.Emoji -> {
                    drawCenteredLabel(canvas, item.rect, "😊", specialKeyPaint)
                }
                is KeyType.Comma -> {
                    drawKeyWithHint(canvas, item.rect, ",", "⚙")
                }
                is KeyType.Space -> {
                    drawCenteredLabel(canvas, item.rect, getSpacebarLabel(), spacebarPaint)
                }
                is KeyType.Dari -> {
                    drawKeyWithHint(canvas, item.rect, "।", ".")
                }
                is KeyType.Enter -> {
                    specialKeyPaint.color = keyAccentColor
                    drawCenteredLabel(canvas, item.rect, enterActionIcon, specialKeyPaint)
                    specialKeyPaint.color = effectiveTextColor
                }
            }
        }
    }

    protected open fun drawKeyboardBackground(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val bgBitmap = currentBgBitmap
        if (bgBitmap != null && !bgBitmap.isRecycled) {
            val srcRect = Rect(0, 0, bgBitmap.width, bgBitmap.height)
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(bgBitmap, srcRect, dstRect, null)

            val overlayAlpha = (currentBgOpacity.coerceIn(0f, 0.95f) * 255).toInt()
            bgOverlayPaint.color = Color.BLACK
            bgOverlayPaint.alpha = overlayAlpha
            canvas.drawRect(0f, 0f, w, h, bgOverlayPaint)
        } else {
            if (currentTheme.isGradient) {
                val gradient = LinearGradient(
                    0f, 0f, w, h,
                    currentTheme.gradientStartColor,
                    currentTheme.gradientEndColor,
                    Shader.TileMode.CLAMP
                )
                keyboardBgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, keyboardBgPaint)
                keyboardBgPaint.shader = null
            } else {
                keyboardBgPaint.color = keyboardBackgroundColor
                canvas.drawRect(0f, 0f, w, h, keyboardBgPaint)
            }
        }
    }

    protected open fun drawKeyBackground(canvas: Canvas, item: BaseKeyItem, isPressed: Boolean) {
        val bgPaint = when {
            isPressed -> keyPressedBackgroundPaint
            item.type is KeyType.Shift -> shiftKeyBackgroundPaint
            item.type is KeyType.Enter -> shiftKeyBackgroundPaint
            item.type is KeyType.Character || item.type is KeyType.Space ||
                    item.type is KeyType.Comma || item.type is KeyType.Dari -> keyBackgroundPaint
            else -> specialKeyBackgroundPaint
        }

        canvas.drawRoundRect(item.rectF, keyCornerRadius, keyCornerRadius, bgPaint)

        // Light theme subtle border on main typing keys
        if (!isDarkTheme && (item.type is KeyType.Character || item.type is KeyType.Space ||
                    item.type is KeyType.Comma || item.type is KeyType.Dari)) {
            canvas.drawRoundRect(item.rectF, keyCornerRadius, keyCornerRadius, keyStrokePaint)
        }
    }

    protected open fun drawShiftKey(canvas: Canvas, item: BaseKeyItem) {
        val shiftLabel = if (isCapsLock) "⇪" else "⬆"
        drawCenteredLabel(canvas, item.rect, shiftLabel, shiftIconPaint)
    }

    protected fun drawCenteredLabel(canvas: Canvas, rect: Rect, label: String, paint: Paint) {
        val cleanLabel = stripDottedCircle(label)
        paint.getTextBounds(cleanLabel, 0, cleanLabel.length, labelTextBounds)
        val centerX = rect.centerX().toFloat()
        val centerY = rect.centerY().toFloat() + (labelTextBounds.height() / 2f)
        canvas.drawText(cleanLabel, centerX, centerY, paint)
    }

    protected fun drawKeyWithHint(canvas: Canvas, rect: Rect, primaryLabel: String, hintLabel: String) {
        val cleanPrimary = stripDottedCircle(primaryLabel)
        primaryPaint.color = effectiveTextColor
        primaryPaint.getTextBounds(cleanPrimary, 0, cleanPrimary.length, labelTextBounds)
        val centerX = rect.centerX().toFloat()
        val centerY = rect.centerY().toFloat() + (labelTextBounds.height() / 2f)
        canvas.drawText(cleanPrimary, centerX, centerY, primaryPaint)

        if (hintLabel.isNotEmpty()) {
            val cleanHint = stripDottedCircle(hintLabel)
            hintPaint.color = keySublabelColor
            hintPaint.getTextBounds(cleanHint, 0, cleanHint.length, labelTextBounds)
            val hintX = rect.right - (labelTextBounds.width() * 1.3f)
            val hintY = rect.top + (labelTextBounds.height() * 1.5f)
            canvas.drawText(cleanHint, hintX, hintY, hintPaint)
        }
    }

    // --- Touch Event Pipeline ---
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = x
                touchDownY = y
                isSpaceSwipeHandled = false
                val hitKey = findKeyAt(x.toInt(), y.toInt())
                if (hitKey != null) {
                    activePressedKey = hitKey
                    isLongPressTriggered = false
                    playKeyFeedback()

                    if (hitKey.type is KeyType.Backspace) {
                        onBackspaceListener?.invoke()
                        startBackspaceRepeat()
                    } else {
                        uiHandler.postDelayed(longPressRunnable, longPressTimeout)
                    }
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - touchDownX
                val dy = y - touchDownY

                // Spacebar horizontal swipe gesture to switch language/layout
                if (activePressedKey?.type is KeyType.Space && !isSpaceSwipeHandled) {
                    if (abs(dx) > minSwipeDistance && abs(dx) > abs(dy) * 1.2f) {
                        isSpaceSwipeHandled = true
                        uiHandler.removeCallbacks(longPressRunnable)
                        activePressedKey = null
                        playKeyFeedback()
                        invalidate()

                        if (dx > 0) {
                            onSpacebarSwipeNextListener?.invoke()
                        } else {
                            onSpacebarSwipePrevListener?.invoke()
                        }
                        return true
                    }
                }

                // Stop backspace if dragged outside bounds
                if (activePressedKey?.type is KeyType.Backspace && !activePressedKey!!.rect.contains(x.toInt(), y.toInt())) {
                    stopBackspaceRepeat()
                    activePressedKey = null
                    invalidate()
                    return true
                }

                val currentHit = findKeyAt(x.toInt(), y.toInt())
                if (currentHit != activePressedKey) {
                    uiHandler.removeCallbacks(longPressRunnable)
                    stopBackspaceRepeat()
                    activePressedKey = currentHit
                    isLongPressTriggered = false
                    if (currentHit != null && !isSpaceSwipeHandled) {
                        playKeyFeedback()
                        if (currentHit.type is KeyType.Backspace) {
                            onBackspaceListener?.invoke()
                            startBackspaceRepeat()
                        } else {
                            uiHandler.postDelayed(longPressRunnable, longPressTimeout)
                        }
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                uiHandler.removeCallbacks(longPressRunnable)
                stopBackspaceRepeat()
                val key = activePressedKey
                activePressedKey = null

                if (isSpaceSwipeHandled) {
                    isSpaceSwipeHandled = false
                    invalidate()
                    return true
                }

                if (key != null && !isLongPressTriggered && key.type !is KeyType.Backspace) {
                    handleKeyTap(key)
                }
                isLongPressTriggered = false
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                uiHandler.removeCallbacks(longPressRunnable)
                stopBackspaceRepeat()
                activePressedKey = null
                isLongPressTriggered = false
                isSpaceSwipeHandled = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    protected fun findKeyAt(x: Int, y: Int): BaseKeyItem? {
        return keyItems.firstOrNull { it.rect.contains(x, y) }
    }

    protected open fun handleKeyTap(item: BaseKeyItem) {
        when (val type = item.type) {
            is KeyType.Character -> {
                handleCharacterTap(item, type.payload)
            }
            is KeyType.Shift -> {
                isShifted = !isShifted
                onShiftToggleListener?.invoke(isShifted)
                invalidate()
            }
            is KeyType.Backspace -> {
                onBackspaceListener?.invoke()
            }
            is KeyType.Symbols -> {
                onSymbolsToggleListener?.invoke()
            }
            is KeyType.Emoji -> {
                onEmojiToggleListener?.invoke()
            }
            is KeyType.Comma -> {
                onKeyCommitListener?.invoke(",")
            }
            is KeyType.Space -> {
                onSpaceListener?.invoke()
            }
            is KeyType.Dari -> {
                onKeyCommitListener?.invoke("।")
            }
            is KeyType.Enter -> {
                onEnterListener?.invoke()
            }
        }
    }

    protected open fun handleKeyLongPress(item: BaseKeyItem) {
        when (val type = item.type) {
            is KeyType.Character -> {
                handleCharacterLongPress(item, type.payload)
            }
            is KeyType.Shift -> {
                isCapsLock = !isCapsLock
                isShifted = isCapsLock
                onShiftToggleListener?.invoke(isShifted)
            }
            is KeyType.Comma -> {
                onSettingsHintListener?.invoke()
            }
            is KeyType.Dari -> {
                onKeyCommitListener?.invoke(".")
            }
            else -> {}
        }
    }

    // --- Layout Engine & Bounds Pipeline ---
    abstract fun calculateKeyBounds(width: Int, height: Int)
    abstract fun getSpacebarLabel(): String

    open fun drawCharacterKey(canvas: Canvas, item: BaseKeyItem, payload: Any, isShifted: Boolean) {
        if (payload is KeyboardKey) {
            val primary = if (isShifted || isCapsLock) {
                if (payload.shiftOutput.isNotEmpty() && payload.shiftOutput != payload.output) {
                    payload.shiftOutput
                } else if (payload.output.length == 1 && payload.output[0].isLetter()) {
                    payload.output.uppercase()
                } else {
                    payload.label
                }
            } else {
                if (payload.output.length == 1 && payload.output[0].isLetter()) {
                    payload.output.lowercase()
                } else {
                    payload.label
                }
            }
            val hint = if (isShifted || isCapsLock) {
                if (payload.shiftOutput.isNotEmpty() && payload.shiftOutput != payload.output) payload.label else ""
            } else {
                payload.hint ?: ""
            }
            drawKeyWithHint(canvas, item.rect, primary, hint)
        }
    }

    open fun handleCharacterTap(item: BaseKeyItem, payload: Any) {
        if (payload is KeyboardKey) {
            val text = if (isShifted || isCapsLock) {
                if (payload.shiftOutput.isNotEmpty()) payload.shiftOutput
                else if (payload.output.length == 1 && payload.output[0].isLetter()) payload.output.uppercase()
                else payload.output
            } else {
                if (payload.output.length == 1 && payload.output[0].isLetter()) payload.output.lowercase()
                else payload.output
            }
            onKeyCommitListener?.invoke(text)
            if (isShifted && !isCapsLock) {
                isShifted = false
                onShiftToggleListener?.invoke(false)
            }
        }
    }

    open fun handleCharacterLongPress(item: BaseKeyItem, payload: Any) {
        if (payload is KeyboardKey) {
            val text = payload.hint ?: (if (payload.shiftOutput.isNotEmpty()) payload.shiftOutput else payload.output)
            onKeyCommitListener?.invoke(text)
        }
    }

    /**
     * Standardized multi-row Canvas key bound builder for all KeyboardKey-based layouts.
     */
    protected fun buildKeyBoundsFromLayout(
        rows: List<List<KeyboardKey>>,
        width: Int,
        height: Int
    ) {
        keyItems.clear()
        if (width <= 0 || height <= 0 || rows.isEmpty()) return

        val density = context.resources.displayMetrics.density
        val marginH = 2f * density
        val marginV = if (isCompactMode) 1f * density else 2f * density
        val gapH = marginH * 2f
        val gapV = marginV * 2f

        val bottomSafetyPadding = if (isCompactMode) 8f * density else 2f * density
        val availableHeight = (height - bottomSafetyPadding).coerceAtLeast(100f)
        val numRows = rows.size
        val totalVerticalGaps = gapV * (numRows + 1)
        val rowHeight = (availableHeight - totalVerticalGaps) / numRows

        var top = gapV

        for (row in rows) {
            val bottom = top + rowHeight
            val totalWeights = row.sumOf { it.weight.toDouble() }.toFloat()
            val totalGaps = gapH * (row.size + 1)
            val unitWidth = (width - totalGaps) / totalWeights

            var left = gapH
            for (key in row) {
                val keyW = key.weight * unitWidth
                val right = left + keyW
                val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                val rectF = RectF(rect)

                val keyType: KeyType = when (key.type) {
                    com.example.ime.KeyType.SHIFT -> KeyType.Shift
                    com.example.ime.KeyType.BACKSPACE -> KeyType.Backspace
                    com.example.ime.KeyType.SPACE -> KeyType.Space
                    com.example.ime.KeyType.ENTER -> KeyType.Enter
                    com.example.ime.KeyType.SWITCH_SYMBOLS -> KeyType.Symbols
                    com.example.ime.KeyType.SWITCH_EMOJI -> KeyType.Emoji
                    else -> {
                        when (key.label) {
                            "," -> KeyType.Comma
                            "।", "." -> KeyType.Dari
                            else -> KeyType.Character(key)
                        }
                    }
                }

                keyItems.add(BaseKeyItem(rect, rectF, keyType))
                left = right + gapH
            }
            top = bottom + gapV
        }
    }

    companion object {
        const val BACKSPACE_INITIAL_DELAY_MS = 400L
        const val BACKSPACE_REPEAT_INTERVAL_MS = 50L

        /**
         * Strips dotted circle (U+25CC / ◌) from vowel signs so isolated Kar-signs render cleanly.
         */
        @JvmStatic
        fun stripDottedCircle(glyph: String): String {
            return glyph.replace("\u25CC", "")
        }
    }
}
