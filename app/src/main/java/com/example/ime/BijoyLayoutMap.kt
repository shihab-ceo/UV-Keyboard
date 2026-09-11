package com.example.ime

/**
 * Standard Bijoy Classic Keyboard Layout Character Mapping.
 *
 * Strict Layout & Character Rules:
 * - Row 3 Key 4 (Index 3 of character keys, corresponding to QWERTY 'V'):
 *   Primary Label = 'ব', Upper Shift / Long-Press Hint = 'র' (Standard Bijoy mapping, duplicate 'র' bug fixed).
 */
object BijoyLayoutMap {

    fun getRows(): List<List<KeyboardKey>> {
        val row1Hints = listOf("ং", "য়", "ঢ", "ফ", "ঠ", "ছ", "ঝ", "ঞ", "ঘ", "ঢ়")
        val row1Main  = listOf("ঙ", "য", "ড", "প", "ট", "চ", "জ", "হ", "গ", "ড়")
        val row1 = row1Main.mapIndexed { idx, char ->
            KeyboardKey(
                label = char,
                hint = row1Hints.getOrNull(idx),
                output = char,
                shiftOutput = row1Hints.getOrElse(idx) { char }
            )
        }

        val row2Hints = listOf("ঋ", "ূ", "ী", "ল", "।", "ভ", "খ", "থ", "ধ")
        val row2Main  = listOf("ৃ", "ু", "ি", "া", "্", "ব", "ক", "ত", "দ")
        val row2 = row2Main.mapIndexed { idx, char ->
            KeyboardKey(
                label = char,
                hint = row2Hints.getOrNull(idx),
                output = char,
                shiftOutput = row2Hints.getOrElse(idx) { char }
            )
        }

        // Row 3 Character Keys:
        // Key 1 (Z): ৲ / ্র
        // Key 2 (X): ো / ৌ
        // Key 3 (C): ে / ৈ
        // Key 4 (V): ব / র  -> Primary = 'ব', Upper Shift / Long-Press Hint = 'র'
        // Key 5 (B): ন / ণ
        // Key 6 (N): স / ষ
        // Key 7 (M): ম / শ
        val row3Hints = listOf("্র", "ৌ", "ৈ", "র", "ণ", "ষ", "শ")
        val row3Main  = listOf("৲", "ো", "ে", "ব", "ন", "স", "ম")
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(KeyboardKey(label = "⬆", hint = null, output = "", type = KeyType.SHIFT, weight = 1.3f))
        row3Main.forEachIndexed { idx, char ->
            val hint = row3Hints.getOrNull(idx)
            row3Keys.add(KeyboardKey(label = char, hint = hint, output = char, shiftOutput = hint ?: char))
        }
        row3Keys.add(KeyboardKey(label = "⌫", hint = null, output = "", type = KeyType.BACKSPACE, weight = 1.3f))

        val row4 = listOf(
            KeyboardKey(label = "?123", output = "", type = KeyType.SWITCH_SYMBOLS, weight = 1.2f),
            KeyboardKey(label = "😊", output = "", type = KeyType.SWITCH_EMOJI, weight = 1.0f),
            KeyboardKey(label = ",", output = ",", weight = 0.9f),
            KeyboardKey(label = "◀  বিজয়  ▶", output = " ", type = KeyType.SPACE, weight = 3.8f),
            KeyboardKey(label = "।", output = "।", hint = ".", shiftOutput = ".", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )

        return listOf(row1, row2, row3Keys, row4)
    }
}
