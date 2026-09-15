package com.example.keyboard.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.keyboard.engine.JatiyaLayoutEngine
import com.example.keyboard.engine.KeyMapping

class KeyRenderer(
    var primaryPaint: Paint,
    var hintPaint: Paint,
    private val engine: JatiyaLayoutEngine = JatiyaLayoutEngine()
) {

    private val textBounds = Rect()

    fun updatePaints(primary: Paint, hint: Paint) {
        this.primaryPaint = primary
        this.hintPaint = hint
    }

    fun drawKey(
        canvas: Canvas,
        keyRect: Rect,
        mapping: KeyMapping,
        isShifted: Boolean
    ) {
        val (rawPrimary, rawHint) = engine.resolveLabels(mapping, isShifted)
        val primaryLabel = BaseKeyboardView.stripDottedCircle(rawPrimary)
        val hintLabel = BaseKeyboardView.stripDottedCircle(rawHint)

        // Draw Primary Center Label
        primaryPaint.getTextBounds(primaryLabel, 0, primaryLabel.length, textBounds)
        val centerX = keyRect.centerX().toFloat()
        val centerY = keyRect.centerY().toFloat() + (textBounds.height() / 2f)
        
        canvas.drawText(primaryLabel, centerX, centerY, primaryPaint)

        // Draw Secondary Top-Right Hint Label (only if distinct from primary)
        if (hintLabel.isNotEmpty() && hintLabel != primaryLabel) {
            hintPaint.getTextBounds(hintLabel, 0, hintLabel.length, textBounds)
            val hintX = keyRect.right - (textBounds.width() * 1.2f)
            val hintY = keyRect.top + (textBounds.height() * 1.5f)
            
            canvas.drawText(hintLabel, hintX, hintY, hintPaint)
        }
    }
}
