package com.example.keyboard.ui

import android.content.Context
import android.util.AttributeSet
import com.example.ime.BengaliLayouts

/**
 * Custom High-Performance Canvas Keyboard View for Bengali "Bijoy" Classic layout.
 *
 * Inherits 100% theme property inheritance (Corner Radius, Opacity, Custom Accent Colors,
 * Dynamic Shift Key styling) directly from [BaseKeyboardView].
 */
class BijoyKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseKeyboardView(context, attrs, defStyleAttr) {

    override fun getSpacebarLabel(): String = "◀  বিজয়  ▶"

    override fun calculateKeyBounds(width: Int, height: Int) {
        buildKeyBoundsFromLayout(BengaliLayouts.getBijoyRows(), width, height)
    }
}
