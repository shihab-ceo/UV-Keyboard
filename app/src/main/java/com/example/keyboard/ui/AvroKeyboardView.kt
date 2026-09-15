package com.example.keyboard.ui

import android.content.Context
import android.util.AttributeSet
import com.example.ime.BengaliLayouts

/**
 * Custom High-Performance Canvas Keyboard View for Bengali "Avro" Phonetic layout.
 *
 * Inherits 100% theme property inheritance and dynamic Shift Key styling from [BaseKeyboardView].
 */
class AvroKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseKeyboardView(context, attrs, defStyleAttr) {

    override fun getSpacebarLabel(): String = "◀  Avro ফোনেটিক  ▶"

    override fun calculateKeyBounds(width: Int, height: Int) {
        buildKeyBoundsFromLayout(BengaliLayouts.getAvroRows(), width, height)
    }
}
