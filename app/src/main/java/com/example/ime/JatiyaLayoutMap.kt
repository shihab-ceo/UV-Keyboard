package com.example.ime

/**
 * Official Jatiya Layout (BDS 1738) Character Mapping.
 *
 * Strict Layout & Character Rules:
 * 1. Jatiya Layout (BDS 1738) - Row 3 Key 4:
 *    - Primary Label (Main Key) = 'র'
 *    - Upper Shift / Long-Press Hint (Top Label) = 'ল'
 *    - Pressing outputs 'র', holding/shifting outputs 'ল'.
 * 2. Jatiya Layout - Row 2 Key 3:
 *    - Primary Label = 'ি' (Short I Kar)
 *    - Shift / Long-Press Hint = 'ী' (Long I Kar)
 */
object JatiyaLayoutMap {

    fun getRows(): List<List<KeyboardKey>> {
        val row1Hints = listOf("ং", "য়", "ট", "ফ", "ঠ", "ছ", "ব", "ঞ", "ঘ", "ঢ়")
        val row1Main  = listOf("ঙ", "য", "ড", "প", "ট", "চ", "জ", "হ", "গ", "ড়")
        val row1 = row1Main.mapIndexed { idx, char ->
            val hint = row1Hints.getOrNull(idx)
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = hint ?: char)
        }

        // Row 2 Keys:
        // Key 3: Primary = 'ি' (Short I Kar), Shift/Long-Press Hint = 'ী' (Long I Kar)
        val row2Mapping = listOf(
            Triple("<", "ঐ", "ঐ"),
            Triple("০", "উ", "উ"),
            Triple("ি", "ী", "ী"),
            Triple("ব", "ভ", "ভ"),
            Triple("্", "আ", "আ"),
            Triple("া", "অ", "অ"),
            Triple("ক", "খ", "খ"),
            Triple("ত", "থ", "থ"),
            Triple("দ", "ধ", "ধ")
        )
        val row2 = row2Mapping.map { (char, hint, shift) ->
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = shift)
        }

        // Row 3 Keys:
        // Key 1: Shift ("⬆")
        // Key 2: ো (hint ও)
        // Key 3: ে (hint এ)
        // Key 4: র (hint ল, shiftOutput ল) -> Pressing outputs 'র', holding/shifting outputs 'ল'
        // Key 5: ন (hint ণ)
        // Key 6: স (hint ষ)
        // Key 7: ম (hint শ)
        // Key 8: Backspace ("⌫")
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(KeyboardKey(label = "⬆", hint = null, output = "", type = KeyType.SHIFT, weight = 1.3f))
        val row3Mapping = listOf(
            Triple("ো", "ও", "ও"),
            Triple("ে", "এ", "এ"),
            Triple("র", "ল", "ল"),
            Triple("ন", "ণ", "ণ"),
            Triple("স", "ষ", "ষ"),
            Triple("ম", "শ", "শ")
        )
        row3Mapping.forEach { (char, hint, shift) ->
            row3Keys.add(KeyboardKey(label = char, hint = hint, output = char, shiftOutput = shift))
        }
        row3Keys.add(KeyboardKey(label = "⌫", hint = null, output = "", type = KeyType.BACKSPACE, weight = 1.3f))

        val row4 = listOf(
            KeyboardKey(label = "?123", output = "", type = KeyType.SWITCH_SYMBOLS, weight = 1.2f),
            KeyboardKey(label = "😊", output = "", type = KeyType.SWITCH_EMOJI, weight = 1.0f),
            KeyboardKey(label = ",", output = ",", hint = "ঃ", shiftOutput = "ঃ", weight = 0.9f),
            KeyboardKey(label = "◀  জাতীয়  ▶", output = " ", type = KeyType.SPACE, weight = 3.8f),
            KeyboardKey(label = ".", output = ".", hint = "।", shiftOutput = "।", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )

        return listOf(row1, row2, row3Keys, row4)
    }
}
