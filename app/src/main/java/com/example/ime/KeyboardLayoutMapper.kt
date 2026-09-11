package com.example.ime

import com.example.ThemeManager

/**
 * Standard Keyboard Layout Mapper for UV Keyboard.
 * Strictly adheres to official standard layouts:
 * - Official Jatiya BDS 1738
 * - English US QWERTY
 * - Avro Phonetic
 * - Bijoy Classic
 * - Prabhat Layout
 */
object KeyboardLayoutMapper {

    fun getLayoutRows(
        layoutMode: String,
        isShifted: Boolean,
        isCapsLock: Boolean,
        actionIcon: String
    ): List<List<KeyboardKey>> {
        val baseRows = when (layoutMode) {
            ThemeManager.LAYOUT_JATIYA -> BengaliLayouts.getJatiyaRows()
            ThemeManager.LAYOUT_AVRO -> BengaliLayouts.getAvroRows()
            ThemeManager.LAYOUT_BIJOY -> BengaliLayouts.getBijoyRows()
            ThemeManager.LAYOUT_PRABHAT -> BengaliLayouts.getPrabhatRows()
            ThemeManager.LAYOUT_ENGLISH -> BengaliLayouts.getEnglishRows()
            else -> BengaliLayouts.getJatiyaRows()
        }

        return baseRows.mapIndexed { rowIndex, row ->
            row.map { key ->
                var updatedKey = key

                // Handle Shift key icon
                if (key.type == KeyType.SHIFT) {
                    val shiftLabel = if (isCapsLock) "⇪" else "⬆"
                    updatedKey = updatedKey.copy(label = shiftLabel)
                }

                // Handle English uppercase transformation when shifted or caps locked
                if (layoutMode == ThemeManager.LAYOUT_ENGLISH || layoutMode == ThemeManager.LAYOUT_AVRO) {
                    if (key.type == KeyType.NORMAL && key.output.length == 1 && key.output[0].isLetter()) {
                        val displayChar = if (isShifted || isCapsLock) key.output.uppercase() else key.output.lowercase()
                        updatedKey = updatedKey.copy(
                            label = displayChar,
                            output = displayChar
                        )
                    }
                }

                // Handle Dynamic Action Key (Enter/Search/Go/Send/Next) in bottom row
                if (key.type == KeyType.ENTER) {
                    updatedKey = updatedKey.copy(label = actionIcon)
                }

                updatedKey
            }
        }
    }
}
