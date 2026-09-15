package com.example.keyboard.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Dedicated Crash-Proof Visual Cursor Trackpad Panel.
 *
 * Provides:
 * 1. Visual touchpad canvas with subtle grid and touch-following reticle.
 * 2. 2D swipe cursor navigation (Left/Right/Up/Down via KEYCODE_DPAD_*) with haptic feedback.
 * 3. Dynamic [inputConnectionProvider] ensuring live, non-null InputConnection from IME service.
 * 4. Integrated text editing utilities with complete IPC exception isolation:
 *    [ Select All ] [ Copy ] [ Cut ] [ Paste ] [ Home ] [ End ] [ ✖ Close ]
 */
class CursorTrackpadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CursorTrackpadView"
    }

    var inputConnectionProvider: (() -> InputConnection?)? = null
    var inputConnection: InputConnection? = null
    var onCloseClicked: (() -> Unit)? = null

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private var touchpadSurface: TouchpadSurfaceView

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        setBackgroundColor(Color.parseColor("#0F172A"))

        // 1. Top Header Bar
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(34))
            setBackgroundColor(Color.parseColor("#182234"))
            setPadding(dp(12), 0, dp(8), 0)
        }

        val tvIcon = TextView(context).apply {
            text = "❖"
            textSize = 15f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, dp(8), 0)
        }
        header.addView(tvIcon)

        val tvTitle = TextView(context).apply {
            text = "Cursor Trackpad"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F8FAFC"))
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(tvTitle)

        val tvHint = TextView(context).apply {
            text = "Swipe to steer cursor"
            textSize = 11f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 0, dp(8), 0)
        }
        header.addView(tvHint)

        val btnClose = TextView(context).apply {
            text = "✖"
            textSize = 14f
            setTextColor(Color.parseColor("#EF4444"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(32), dp(28))
            background = createRippleDrawable(Color.parseColor("#2D1D27"), Color.parseColor("#EF4444"))
            contentDescription = "Close Cursor Trackpad"
            setOnClickListener {
                onCloseClicked?.invoke()
            }
        }
        header.addView(btnClose)

        addView(header)

        // 2. Center Touchpad Area: dynamically expands (weight = 1f) to occupy 100% of the keyboard container space
        touchpadSurface = TouchpadSurfaceView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(dp(8), dp(4), dp(8), dp(4))
            }
            onCursorStep = { dpadKey ->
                sendDpadEvent(dpadKey)
            }
        }
        addView(touchpadSurface)

        // 3. Integrated Text Editing Utilities Row (Horizontal scroll)
        val utilitiesScroll = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(38)).apply {
                bottomMargin = dp(4)
            }
            isHorizontalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
        }

        val utilitiesBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            setPadding(dp(8), 0, dp(8), 0)
        }

        // Action Buttons: Select All, Copy, Cut, Paste, Home, End, Close
        utilitiesBar.addView(createActionButton("Select All", "✓") {
            performContextMenuAction(android.R.id.selectAll)
            triggerHaptic()
        })

        utilitiesBar.addView(createActionButton("Copy", "📋") {
            performContextMenuAction(android.R.id.copy)
            triggerHaptic()
        })

        utilitiesBar.addView(createActionButton("Cut", "✂") {
            performContextMenuAction(android.R.id.cut)
            triggerHaptic()
        })

        utilitiesBar.addView(createActionButton("Paste", "📥") {
            performContextMenuAction(android.R.id.paste)
            triggerHaptic()
        })

        utilitiesBar.addView(createActionButton("Home", "⏮") {
            sendKeyEvent(KeyEvent.KEYCODE_MOVE_HOME)
            triggerHaptic()
        })

        utilitiesBar.addView(createActionButton("End", "⏭") {
            sendKeyEvent(KeyEvent.KEYCODE_MOVE_END)
            triggerHaptic()
        })

        utilitiesBar.addView(createActionButton("Close", "✖", isDestructive = true) {
            onCloseClicked?.invoke()
        })

        utilitiesScroll.addView(utilitiesBar)
        addView(utilitiesScroll)
    }

    private fun getSafeInputConnection(): InputConnection? {
        return try {
            inputConnectionProvider?.invoke() ?: inputConnection
        } catch (e: Exception) {
            Log.w(TAG, "Exception resolving InputConnection", e)
            null
        }
    }

    private fun createActionButton(
        label: String,
        icon: String,
        isDestructive: Boolean = false,
        onClick: () -> Unit
    ): LinearLayout {
        val btn = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(36)).apply {
                marginEnd = dp(6)
            }
            setPadding(dp(10), 0, dp(10), 0)
            val bgColor = if (isDestructive) Color.parseColor("#261721") else Color.parseColor("#1E293B")
            val rippleColor = if (isDestructive) Color.parseColor("#EF4444") else Color.parseColor("#38BDF8")
            background = createRippleDrawable(bgColor, rippleColor)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onClick()
            }
        }

        val tvIcon = TextView(context).apply {
            text = icon
            textSize = 12f
            setTextColor(if (isDestructive) Color.parseColor("#EF4444") else Color.parseColor("#38BDF8"))
            setPadding(0, 0, dp(4), 0)
        }
        btn.addView(tvIcon)

        val tvText = TextView(context).apply {
            text = label
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (isDestructive) Color.parseColor("#EF4444") else Color.parseColor("#F1F5F9"))
        }
        btn.addView(tvText)

        return btn
    }

    private fun sendDpadEvent(keyCode: Int) {
        val ic = getSafeInputConnection() ?: return
        try {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            triggerHaptic()
        } catch (e: Exception) {
            Log.w(TAG, "Safe catch on DPAD sendKeyEvent", e)
        }
    }

    private fun sendKeyEvent(keyCode: Int) {
        val ic = getSafeInputConnection() ?: return
        try {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } catch (e: Exception) {
            Log.w(TAG, "Safe catch on sendKeyEvent", e)
        }
    }

    private fun performContextMenuAction(actionId: Int) {
        val ic = getSafeInputConnection() ?: return
        try {
            ic.performContextMenuAction(actionId)
        } catch (e: Exception) {
            Log.w(TAG, "Safe catch on performContextMenuAction", e)
        }
    }

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(14, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(14)
            }
        } catch (_: Exception) {}
    }

    private fun createRippleDrawable(normalColor: Int, rippleColor: Int): RippleDrawable {
        val shape = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(normalColor)
            setStroke(dp(1), Color.parseColor("#334155"))
        }
        val mask = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(Color.WHITE)
        }
        return RippleDrawable(ColorStateList.valueOf(rippleColor), shape, mask)
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * Custom Canvas-based Touchpad Surface.
     * Features:
     * - Subtle geometric grid lines.
     * - Glowing active touch reticle following user touch position.
     * - Compass navigation glyphs.
     * - Smooth 2D step accumulator triggering cursor steps safely without main thread lockup.
     */
    private class TouchpadSurfaceView(context: Context) : View(context) {

        var onCursorStep: ((Int) -> Unit)? = null

        private var touchX = -1f
        private var touchY = -1f
        private var isTouching = false

        private var lastX = 0f
        private var lastY = 0f
        private var accumulatedDx = 0f
        private var accumulatedDy = 0f

        // 14dp per cursor step provides precise, natural steering
        private val stepPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            14f,
            resources.displayMetrics
        )

        private val bgPaint = Paint().apply {
            color = Color.parseColor("#131C2E")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val borderPaint = Paint().apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        private val gridPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
            isAntiAlias = true
        }

        private val centerGlyphPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        private val centerHintPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        private val touchReticlePaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        private val touchFillPaint = Paint().apply {
            color = Color.parseColor("#2638BDF8")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            val radius = 24f

            // 1. Draw rounded background
            canvas.drawRoundRect(0f, 0f, w, h, radius, radius, bgPaint)

            // 2. Draw subtle grid lines
            val gridStep = 48f
            var x = gridStep
            while (x < w) {
                canvas.drawLine(x, 0f, x, h, gridPaint)
                x += gridStep
            }
            var y = gridStep
            while (y < h) {
                canvas.drawLine(0f, y, w, y, gridPaint)
                y += gridStep
            }

            // 3. Draw border
            canvas.drawRoundRect(0f, 0f, w, h, radius, radius, borderPaint)

            // 4. Draw center compass guide when not touching
            if (!isTouching) {
                canvas.drawText("◀   ▲   ▼   ▶", w / 2f, h / 2f - 4f, centerGlyphPaint)
                canvas.drawText("SWIPE TO MOVE CURSOR", w / 2f, h / 2f + 36f, centerHintPaint)
            } else {
                // Draw glowing active reticle at touch point
                canvas.drawCircle(touchX, touchY, 32f, touchFillPaint)
                canvas.drawCircle(touchX, touchY, 32f, touchReticlePaint)
                canvas.drawCircle(touchX, touchY, 4f, touchReticlePaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isTouching = true
                    touchX = event.x
                    touchY = event.y
                    lastX = event.x
                    lastY = event.y
                    accumulatedDx = 0f
                    accumulatedDy = 0f
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    touchX = event.x
                    touchY = event.y

                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    lastX = event.x
                    lastY = event.y

                    accumulatedDx += dx
                    accumulatedDy += dy

                    // Horizontal cursor movement (Left / Right)
                    while (accumulatedDx >= stepPx) {
                        try {
                            onCursorStep?.invoke(KeyEvent.KEYCODE_DPAD_RIGHT)
                        } catch (e: Exception) {
                            Log.w(TAG, "Cursor step right error", e)
                        }
                        accumulatedDx -= stepPx
                    }
                    while (accumulatedDx <= -stepPx) {
                        try {
                            onCursorStep?.invoke(KeyEvent.KEYCODE_DPAD_LEFT)
                        } catch (e: Exception) {
                            Log.w(TAG, "Cursor step left error", e)
                        }
                        accumulatedDx += stepPx
                    }

                    // Vertical cursor movement (Up / Down)
                    while (accumulatedDy >= stepPx) {
                        try {
                            onCursorStep?.invoke(KeyEvent.KEYCODE_DPAD_DOWN)
                        } catch (e: Exception) {
                            Log.w(TAG, "Cursor step down error", e)
                        }
                        accumulatedDy -= stepPx
                    }
                    while (accumulatedDy <= -stepPx) {
                        try {
                            onCursorStep?.invoke(KeyEvent.KEYCODE_DPAD_UP)
                        } catch (e: Exception) {
                            Log.w(TAG, "Cursor step up error", e)
                        }
                        accumulatedDy += stepPx
                    }

                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isTouching = false
                    accumulatedDx = 0f
                    accumulatedDy = 0f
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }
}
