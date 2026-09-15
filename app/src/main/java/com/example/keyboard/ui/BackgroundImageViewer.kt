package com.example.keyboard.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * BackgroundImageViewer provides an interactive pan, pinch-to-zoom, and aspect-ratio
 * cropping view specifically proportioned for custom keyboard background images.
 *
 * Features:
 * 1. Aspect-Ratio Locked Viewport: Formatted to typical keyboard layout proportions (~2.1:1).
 * 2. Gesture Matrix Engine: Smooth two-finger pinch-to-zoom and one-finger translation/pan.
 * 3. Boundary Clamping: Restricts pan and scale to prevent empty voids inside the crop frame.
 * 4. Live Contrast Preview: Displays real-time darkness overlay within the crop frame so users
 *    can verify that keyboard letter keys remain legible on top of the image.
 * 5. Memory-Safe High-Resolution Extraction: Crops the exact bitmap viewport to a clean,
 *    optimized output bitmap.
 */
class BackgroundImageViewer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // Keyboard aspect ratio width / height (standard ~2.1:1)
        const val KEYBOARD_ASPECT_RATIO = 2.1f
        const val MAX_CROPPED_WIDTH = 1080
    }

    private var sourceBitmap: Bitmap? = null
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()

    private val cropRect = RectF()
    private val imageBounds = RectF()
    private val transformedBounds = RectF()

    // Overlay darkness opacity for keys readability (0.0 to 0.95)
    var overlayOpacity: Float = 0.45f
        set(value) {
            field = value.coerceIn(0f, 0.95f)
            invalidate()
        }

    // Touch gesture handling
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY

            // Calculate current scale
            val currentScale = getMatrixScale()
            val minScale = calculateMinScale()
            val maxScale = minScale * 4.0f

            val targetScale = (currentScale * scaleFactor).coerceIn(minScale, maxScale)
            val actualFactor = targetScale / currentScale

            matrix.postScale(actualFactor, actualFactor, focusX, focusY)
            clampMatrixTranslation()
            invalidate()
            return true
        }
    })

    // Drawing paints
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3000000") // 70% black outside crop area
        style = Paint.Style.FILL
    }

    private val cropBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0088CC") // Primary blue accent
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val cornerGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }

    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4DFFFFFF") // 30% white rule-of-thirds grid
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
    }

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val scrimPath = Path()

    fun setImageBitmap(bitmap: Bitmap?) {
        sourceBitmap = bitmap
        if (bitmap != null) {
            imageBounds.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            resetMatrixToFitCropRect()
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateCropRect(w, h)
        resetMatrixToFitCropRect()
    }

    private fun calculateCropRect(viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return

        val horizontalPadding = 32f * resources.displayMetrics.density
        val targetWidth = (viewWidth - (horizontalPadding * 2)).coerceAtLeast(100f)
        val targetHeight = targetWidth / KEYBOARD_ASPECT_RATIO

        val left = (viewWidth - targetWidth) / 2f
        val top = (viewHeight - targetHeight) / 2f
        cropRect.set(left, top, left + targetWidth, top + targetHeight)
    }

    private fun resetMatrixToFitCropRect() {
        val bmp = sourceBitmap ?: return
        if (cropRect.isEmpty) return

        matrix.reset()

        val scaleX = cropRect.width() / bmp.width.toFloat()
        val scaleY = cropRect.height() / bmp.height.toFloat()
        val baseScale = max(scaleX, scaleY)

        matrix.postScale(baseScale, baseScale)

        val scaledW = bmp.width * baseScale
        val scaledH = bmp.height * baseScale
        val dx = cropRect.left + (cropRect.width() - scaledW) / 2f
        val dy = cropRect.top + (cropRect.height() - scaledH) / 2f
        matrix.postTranslate(dx, dy)

        clampMatrixTranslation()
        invalidate()
    }

    private fun getMatrixScale(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun calculateMinScale(): Float {
        val bmp = sourceBitmap ?: return 1f
        val scaleX = cropRect.width() / bmp.width.toFloat()
        val scaleY = cropRect.height() / bmp.height.toFloat()
        return max(scaleX, scaleY)
    }

    private fun clampMatrixTranslation() {
        val bmp = sourceBitmap ?: return
        if (cropRect.isEmpty) return

        transformedBounds.set(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        matrix.mapRect(transformedBounds)

        var dx = 0f
        var dy = 0f

        if (transformedBounds.width() < cropRect.width()) {
            dx = cropRect.centerX() - transformedBounds.centerX()
        } else {
            if (transformedBounds.left > cropRect.left) {
                dx = cropRect.left - transformedBounds.left
            } else if (transformedBounds.right < cropRect.right) {
                dx = cropRect.right - transformedBounds.right
            }
        }

        if (transformedBounds.height() < cropRect.height()) {
            dy = cropRect.centerY() - transformedBounds.centerY()
        } else {
            if (transformedBounds.top > cropRect.top) {
                dy = cropRect.top - transformedBounds.top
            } else if (transformedBounds.bottom < cropRect.bottom) {
                dy = cropRect.bottom - transformedBounds.bottom
            }
        }

        matrix.postTranslate(dx, dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    matrix.postTranslate(dx, dy)
                    clampMatrixTranslation()
                    invalidate()
                }
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw transformed source bitmap
        val bmp = sourceBitmap
        if (bmp != null && !bmp.isRecycled) {
            canvas.drawBitmap(bmp, matrix, bitmapPaint)
        }

        if (cropRect.isEmpty) return

        // 2. Draw live darkness overlay inside crop frame
        if (overlayOpacity > 0.01f) {
            overlayPaint.alpha = (overlayOpacity * 255).toInt()
            canvas.drawRect(cropRect, overlayPaint)
        }

        // 3. Draw exterior darkened scrim
        scrimPath.reset()
        scrimPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        scrimPath.addRect(cropRect, Path.Direction.CCW)
        canvas.drawPath(scrimPath, scrimPaint)

        // 4. Draw rule-of-thirds grid inside crop viewport
        val oneThirdW = cropRect.width() / 3f
        val oneThirdH = cropRect.height() / 3f

        canvas.drawLine(cropRect.left + oneThirdW, cropRect.top, cropRect.left + oneThirdW, cropRect.bottom, gridLinePaint)
        canvas.drawLine(cropRect.left + oneThirdW * 2f, cropRect.top, cropRect.left + oneThirdW * 2f, cropRect.bottom, gridLinePaint)
        canvas.drawLine(cropRect.left, cropRect.top + oneThirdH, cropRect.right, cropRect.top + oneThirdH, gridLinePaint)
        canvas.drawLine(cropRect.left, cropRect.top + oneThirdH * 2f, cropRect.right, cropRect.top + oneThirdH * 2f, gridLinePaint)

        // 5. Draw crop border
        canvas.drawRect(cropRect, cropBorderPaint)

        // 6. Draw corner guides
        val cornerLen = 20f * resources.displayMetrics.density
        // Top-left
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + cornerLen, cropRect.top, cornerGuidePaint)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + cornerLen, cornerGuidePaint)
        // Top-right
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right - cornerLen, cropRect.top, cornerGuidePaint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + cornerLen, cornerGuidePaint)
        // Bottom-left
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + cornerLen, cropRect.bottom, cornerGuidePaint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left, cropRect.bottom - cornerLen, cornerGuidePaint)
        // Bottom-right
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right - cornerLen, cropRect.bottom, cornerGuidePaint)
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right, cropRect.bottom - cornerLen, cornerGuidePaint)
    }

    /**
     * Crops and returns the bitmap within the keyboard crop viewport, scaled down
     * to a performance-friendly maximum width (1080px).
     */
    fun getCroppedBitmap(): Bitmap? {
        val bmp = sourceBitmap ?: return null
        if (cropRect.isEmpty || bmp.isRecycled) return null

        matrix.invert(inverseMatrix)
        val mappedCropRect = RectF()
        inverseMatrix.mapRect(mappedCropRect, cropRect)

        // Clamp to image dimensions
        val cropLeft = mappedCropRect.left.coerceIn(0f, bmp.width.toFloat()).toInt()
        val cropTop = mappedCropRect.top.coerceIn(0f, bmp.height.toFloat()).toInt()
        val cropRight = mappedCropRect.right.coerceIn(0f, bmp.width.toFloat()).toInt()
        val cropBottom = mappedCropRect.bottom.coerceIn(0f, bmp.height.toFloat()).toInt()

        val cropWidth = (cropRight - cropLeft).coerceAtLeast(1)
        val cropHeight = (cropBottom - cropTop).coerceAtLeast(1)

        val cropped = try {
            Bitmap.createBitmap(bmp, cropLeft, cropTop, cropWidth, cropHeight)
        } catch (_: Exception) {
            return null
        }

        // Scale if wider than max resolution to save RAM
        return if (cropped.width > MAX_CROPPED_WIDTH) {
            val targetHeight = (MAX_CROPPED_WIDTH / KEYBOARD_ASPECT_RATIO).toInt()
            val scaled = Bitmap.createScaledBitmap(cropped, MAX_CROPPED_WIDTH, targetHeight, true)
            if (scaled != cropped) {
                cropped.recycle()
            }
            scaled
        } else {
            cropped
        }
    }
}
