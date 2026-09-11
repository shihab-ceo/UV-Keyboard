package com.example.ime

enum class KeyType {
    NORMAL,
    SHIFT,
    BACKSPACE,
    SPACE,
    ENTER,
    SWITCH_MODE,
    SWITCH_SYMBOLS,
    SWITCH_EMOJI,
    CLIPBOARD,
    VOICE
}

data class KeyboardKey(
    val label: String,
    val hint: String? = null,
    val output: String = label,
    val shiftOutput: String = hint ?: label,
    val type: KeyType = KeyType.NORMAL,
    val weight: Float = 1.0f
)
