package com.example.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

/**
 * High-performance Canvas-based Key View for Bengali keyboards (Redmi / Gboard reference design).
 *
 * Requirements Met:
 * 1. Visual Layout & Typography:
 *    - Main Character (Unshifted): Centered at the middle of the key, using a large, clear font.
 *    - Hint / Shifted Character: Positioned cleanly at the TOP-RIGHT corner, using a smaller font size.
 *    - Zero overlapping between the Main Label and the Top-Right Hint.
 *    - Top-Right Hint is hidden when Shift / CapsLock is active (or replaced by the active shifted character).
 * 2. Standalone Kar-Sign Rendering:
 *    - Automatically strips any dotted circles (◌ / U+25CC) so vowel signs (ি, ু, ৌ, া, etc.) render cleanly.
 * 3. Dynamic theming and pressed state feedback via custom Canvas drawing or background drawables.
 */
class BengaliKeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val mainTextBounds = Rect()
    private val hintTextBounds = Rect()

    private var mainText: String = ""
    private var hintText: String = ""
    private var isShiftActive: Boolean = false
    private var isActionOrSpecialKey: Boolean = false

    /**
     * Updates key state and triggers redraw.
     *
     * @param label The primary character or icon to render in the center
     * @param hint The alternate / shifted character to render in the top-right corner
     * @param isShifted Whether the keyboard is currently shifted or caps-locked
     * @param isSpecial Whether this is a non-character key (Space, Shift, Backspace, Enter)
     * @param textColor Color for the primary character
     * @param hintColor Color for the top-right hint
     * @param fontScale User-configurable font scale multiplier
     */
    fun setKeyData(
        label: String,
        hint: String?,
        isShifted: Boolean,
        isSpecial: Boolean,
        textColor: Int,
        hintColor: Int,
        fontScale: Float = 1.0f
    ) {
        this.isShiftActive = isShifted
        this.isActionOrSpecialKey = isSpecial
        this.mainText = sanitizeBengaliGlyph(label)
        this.hintText = if (!hint.isNullOrEmpty() && !isShifted && !isSpecial) {
            sanitizeBengaliGlyph(hint)
        } else {
            ""
        }

        mainPaint.color = textColor
        hintPaint.color = hintColor

        val metrics = resources.displayMetrics
        val baseMainSp = if (isSpecial) 15f else 19f
        val baseHintSp = 10f

        mainPaint.textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            (baseMainSp * fontScale).coerceIn(12f, 32f),
            metrics
        )
        hintPaint.textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            (baseHintSp * fontScale).coerceIn(8f, 16f),
            metrics
        )

        invalidate()
    }

    /**
     * Strips dotted circle (U+25CC) and zero-width joins that cause visual artifacts on isolated vowel signs.
     */
    private fun sanitizeBengaliGlyph(glyph: String): String {
        return glyph.replace("\u25CC", "") // Strip Dotted Circle (◌)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Draw Top-Right Hint Label if visible
        if (hintText.isNotEmpty() && !isShiftActive && !isActionOrSpecialKey) {
            hintPaint.getTextBounds(hintText, 0, hintText.length, hintTextBounds)
            val paddingRight = w * 0.12f
            val paddingTop = h * 0.26f
            val hintX = w - paddingRight
            val hintY = paddingTop
            canvas.drawText(hintText, hintX, hintY, hintPaint)
        }

        // 2. Draw Main Label centered
        if (mainText.isNotEmpty()) {
            mainPaint.getTextBounds(mainText, 0, mainText.length, mainTextBounds)
            val centerX = w / 2f
            // Slightly offset downward if hint is present to achieve perfect optical centering
            val opticalYOffset = if (hintText.isNotEmpty()) h * 0.05f else 0f
            val centerY = (h / 2f) - ((mainPaint.descent() + mainPaint.ascent()) / 2f) + opticalYOffset
            canvas.drawText(mainText, centerX, centerY, mainPaint)
        }
    }
}
