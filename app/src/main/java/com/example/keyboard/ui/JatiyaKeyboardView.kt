package com.example.keyboard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import com.example.keyboard.engine.JatiyaKeyMapping
import com.example.keyboard.engine.JatiyaLayoutEngine
import com.example.keyboard.engine.KeyMapping

/**
 * Custom High-Performance Canvas Keyboard View for Bengali "Jatiya" layout.
 *
 * Inherits all common theme, dynamic colors, custom image background,
 * font scaling, feedback (sound/haptics), gesture, and dimension pipelines
 * from [BaseKeyboardView].
 *
 * Only contains the layout-specific matrix mapping and Jatiya typing engine resolution.
 */
class JatiyaKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseKeyboardView(context, attrs, defStyleAttr) {

    private val engine = JatiyaLayoutEngine()
    private val keyRenderer = KeyRenderer(primaryPaint, hintPaint, engine)

    override fun getSpacebarLabel(): String = "◀  জাতীয়  ▶"

    /**
     * Dynamically calculates bounds for all keys across 4 rows.
     * Horizontal and vertical margins match the standard (2dp on each side = 4dp gap).
     *
     * - Row 1: 10 Character keys from JatiyaKeyMapping.ROW_1
     * - Row 2: 9 Character keys from JatiyaKeyMapping.ROW_2 (horizontally centered)
     * - Row 3: Shift (1.35x) + 7 Character keys from ROW_3 + Backspace (1.35x)
     * - Row 4 (Standard Bijoy control row order):
     *   1. [ ?123 ] (1.2f)
     *   2. [ 😊 ] (1.0f)
     *   3. [ , ] (0.9f)
     *   4. [ ◀  জাতীয়  ▶ ] (3.8f)
     *   5. [ । ] (0.9f)
     *   6. [ Action/Enter ] (1.3f)
     */
    override fun calculateKeyBounds(width: Int, height: Int) {
        keyItems.clear()
        if (width <= 0 || height <= 0) return

        val density = context.resources.displayMetrics.density
        val marginH = 2f * density
        val marginV = if (isCompactMode) 1f * density else 2f * density
        val gapH = marginH * 2f
        val gapV = marginV * 2f

        val bottomSafetyPadding = if (isCompactMode) 8f * density else 2f * density
        val availableHeight = (height - bottomSafetyPadding).coerceAtLeast(100f)
        val numRows = 4
        val totalVerticalGaps = gapV * (numRows + 1)
        val rowHeight = (availableHeight - totalVerticalGaps) / numRows

        // --- ROW 1 (10 Keys) ---
        val r1Keys = JatiyaKeyMapping.ROW_1
        val r1TotalGaps = gapH * (r1Keys.size + 1)
        val r1KeyWidth = (width - r1TotalGaps) / r1Keys.size
        var top = gapV
        var bottom = top + rowHeight
        var left = gapH

        for (mapping in r1Keys) {
            val right = left + r1KeyWidth
            val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            keyItems.add(BaseKeyItem(rect, RectF(rect), KeyType.Character(mapping)))
            left = right + gapH
        }

        // --- ROW 2 (9 Keys - Centered with equal side margins) ---
        val r2Keys = JatiyaKeyMapping.ROW_2
        top = bottom + gapV
        bottom = top + rowHeight
        val r2MarginSide = (width * 0.035f)
        val r2AvailableWidth = width - (r2MarginSide * 2f)
        val r2TotalGaps = gapH * (r2Keys.size - 1)
        val r2KeyWidth = (r2AvailableWidth - r2TotalGaps) / r2Keys.size
        left = r2MarginSide

        for (mapping in r2Keys) {
            val right = left + r2KeyWidth
            val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            keyItems.add(BaseKeyItem(rect, RectF(rect), KeyType.Character(mapping)))
            left = right + gapH
        }

        // --- ROW 3: Shift + 7 Character Keys + Backspace ---
        val r3Keys = JatiyaKeyMapping.ROW_3
        top = bottom + gapV
        bottom = top + rowHeight

        val r3CharCount = r3Keys.size // 7
        val r3TotalUnits = 1.35f + r3CharCount + 1.35f
        val r3TotalGaps = gapH * (r3CharCount + 2)
        val r3UnitWidth = (width - r3TotalGaps) / r3TotalUnits
        val shiftWidth = r3UnitWidth * 1.35f
        val charWidth = r3UnitWidth
        val backspaceWidth = r3UnitWidth * 1.35f

        // Shift Key
        left = gapH
        var right = left + shiftWidth
        var rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        keyItems.add(BaseKeyItem(rect, RectF(rect), KeyType.Shift))
        left = right + gapH

        // 7 Character Keys
        for (mapping in r3Keys) {
            right = left + charWidth
            rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            keyItems.add(BaseKeyItem(rect, RectF(rect), KeyType.Character(mapping)))
            left = right + gapH
        }

        // Backspace Key
        right = width - gapH
        left = right - backspaceWidth
        rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        keyItems.add(BaseKeyItem(rect, RectF(rect), KeyType.Backspace))

        // --- ROW 4 (BIJOY MATCHING CONTROL ROW) ---
        // 1. [ ?123 ] (1.2f) | 2. [ 😊 ] (1.0f) | 3. [ , ] (0.9f) | 4. [ ◀  জাতীয়  ▶ ] (3.8f) | 5. [ । ] (0.9f) | 6. [ Enter ] (1.3f)
        top = bottom + gapV
        bottom = top + rowHeight

        val wSymbols = 1.2f
        val wEmoji = 1.0f
        val wComma = 0.9f
        val wSpace = 3.8f
        val wDari = 0.9f
        val wEnter = 1.3f
        val totalWeights = wSymbols + wEmoji + wComma + wSpace + wDari + wEnter
        val r4TotalGaps = 7 * gapH
        val unitR4 = (width - r4TotalGaps) / totalWeights

        val r4Items = listOf(
            Pair(wSymbols * unitR4, KeyType.Symbols),
            Pair(wEmoji * unitR4, KeyType.Emoji),
            Pair(wComma * unitR4, KeyType.Comma),
            Pair(wSpace * unitR4, KeyType.Space),
            Pair(wDari * unitR4, KeyType.Dari),
            Pair(wEnter * unitR4, KeyType.Enter)
        )

        left = gapH
        for (pair in r4Items) {
            val kWidth = pair.first
            val kType = pair.second
            right = left + kWidth
            rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            keyItems.add(BaseKeyItem(rect, RectF(rect), kType))
            left = right + gapH
        }
    }

    override fun drawCharacterKey(
        canvas: Canvas,
        item: BaseKeyItem,
        payload: Any,
        isShifted: Boolean
    ) {
        if (payload is KeyMapping) {
            keyRenderer.updatePaints(primaryPaint, hintPaint)
            keyRenderer.drawKey(canvas, item.rect, payload, isShifted)
        }
    }

    override fun handleCharacterTap(item: BaseKeyItem, payload: Any) {
        if (payload is KeyMapping) {
            val output = engine.resolveOutput(payload, isShifted, isLongPress = false)
            onKeyCommitListener?.invoke(output)
            if (isShifted) {
                isShifted = false
                onShiftToggleListener?.invoke(false)
            }
        }
    }

    override fun handleCharacterLongPress(item: BaseKeyItem, payload: Any) {
        if (payload is KeyMapping) {
            val output = engine.resolveOutput(payload, isShifted, isLongPress = true)
            onKeyCommitListener?.invoke(output)
        }
    }
}
