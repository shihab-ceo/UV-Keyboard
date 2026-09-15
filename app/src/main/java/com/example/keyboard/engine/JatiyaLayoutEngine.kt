package com.example.keyboard.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/**
 * Data representation of a single key with 4 dynamic states.
 */
data class KeyMapping(
    val unshiftedPrimary: String,
    val unshiftedLongPress: String,
    val shiftedPrimary: String,
    val shiftedLongPress: String
)

/**
 * Key Action Resolver handles character output logic.
 */
class JatiyaLayoutEngine {

    /**
     * Resolves key tap/long-press based on current shift state.
     */
    fun resolveOutput(mapping: KeyMapping, isShifted: Boolean, isLongPress: Boolean): String {
        return if (!isShifted) {
            if (isLongPress) mapping.unshiftedLongPress else mapping.unshiftedPrimary
        } else {
            if (isLongPress) mapping.shiftedLongPress else mapping.shiftedPrimary
        }
    }

    /**
     * Resolves visible labels for rendering on Canvas.
     * Returns a Pair(Primary Center Label, Secondary Hint Label).
     */
    fun resolveLabels(mapping: KeyMapping, isShifted: Boolean): Pair<String, String> {
        val primary = if (!isShifted) mapping.unshiftedPrimary else mapping.shiftedPrimary
        val hint = if (!isShifted) mapping.unshiftedLongPress else mapping.shiftedLongPress
        
        return Pair(
            sanitizeForDrawing(primary),
            sanitizeForDrawing(hint)
        )
    }

    /**
     * Strips U+25CC (Dotted Circle ◌) to prevent rendering artifacts on key surfaces.
     */
    private fun sanitizeForDrawing(text: String): String {
        return text.replace("\u25CC", "")
    }
}
