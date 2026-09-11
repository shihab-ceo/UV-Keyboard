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
        // Row 1 (10 keys): BDS 1738 consonant row
        // Q: ঙ (ং)
        // W: য (য়)
        // E: ড (ঢ)  [Fixed duplicate 'ট' bug]
        // R: প (ফ)
        // T: ট (ঠ)
        // Y: চ (ছ)
        // U: জ (ঝ)  [Fixed duplicate 'ব' and missing 'ঝ' bug]
        // I: হ (ঞ)
        // O: গ (ঘ)
        // P: ড় (ঢ়)
        val row1Hints = listOf("ং", "য়", "ঢ", "ফ", "ঠ", "ছ", "ঝ", "ঞ", "ঘ", "ঢ়")
        val row1Main  = listOf("ঙ", "য", "ড", "প", "ট", "চ", "জ", "হ", "গ", "ড়")
        val row1 = row1Main.mapIndexed { idx, char ->
            val hint = row1Hints.getOrNull(idx)
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = hint ?: char)
        }

        // Row 2 (10 keys):
        // Key 1: Primary = 'ৃ' (Rri Kar), Shift/Hint = 'ঋ' [Fixed '<' bug]
        // Key 2: Primary = 'ু' (Short U Kar), Shift/Hint = 'ূ' [Fixed '০' bug]
        // Key 3: Primary = 'ি' (Short I Kar), Shift/Long-Press Hint = 'ী' (Long I Kar) [Rule 2]
        // Key 4: Primary = 'ব', Shift/Hint = 'ভ'
        // Key 5: Primary = '্' (Hasanta Virama), Shift/Hint = 'ৎ' (Khanda Ta)
        // Key 6: Primary = 'া' (Aa Kar), Shift/Hint = 'অ'
        // Key 7: Primary = 'ক', Shift/Hint = 'খ'
        // Key 8: Primary = 'ত', Shift/Hint = 'থ'
        // Key 9: Primary = 'দ', Shift/Hint = 'ধ'
        // Key 10: Primary = 'ঃ' (Visarga), Shift/Hint = 'ঁ' (Chandrabindu)
        val row2Mapping = listOf(
            Triple("ৃ", "ঋ", "ঋ"),
            Triple("ু", "ূ", "ূ"),
            Triple("ি", "ী", "ী"),
            Triple("ব", "ভ", "ভ"),
            Triple("্", "ৎ", "ৎ"),
            Triple("া", "অ", "অ"),
            Triple("ক", "খ", "খ"),
            Triple("ত", "থ", "থ"),
            Triple("দ", "ধ", "ধ"),
            Triple("ঃ", "ঁ", "ঁ")
        )
        val row2 = row2Mapping.map { (char, hint, shift) ->
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = shift)
        }

        // Row 3 Keys (Shift + 8 Character Keys + Backspace = 10 keys):
        // Key 1: Shift ("⬆")
        // Key 2: ো (hint/shift ও)
        // Key 3: ে (hint/shift এ)
        // Key 4: র (hint/shift ল) [Rule 1: Pressing outputs 'র', holding/shifting outputs 'ল']
        // Key 5: ন (hint/shift ণ)
        // Key 6: স (hint/shift ষ)
        // Key 7: ম (hint/shift শ)
        // Key 8: ৈ (hint/shift ঐ)
        // Key 9: ৌ (hint/shift ঔ)
        // Key 10: Backspace ("⌫")
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(KeyboardKey(label = "⬆", hint = null, output = "", type = KeyType.SHIFT, weight = 1.25f))
        val row3Mapping = listOf(
            Triple("ো", "ও", "ও"),
            Triple("ে", "এ", "এ"),
            Triple("র", "ল", "ল"),
            Triple("ন", "ণ", "ণ"),
            Triple("স", "ষ", "ষ"),
            Triple("ম", "শ", "শ"),
            Triple("ৈ", "ঐ", "ঐ"),
            Triple("ৌ", "ঔ", "ঔ")
        )
        row3Mapping.forEach { (char, hint, shift) ->
            row3Keys.add(KeyboardKey(label = char, hint = hint, output = char, shiftOutput = shift))
        }
        row3Keys.add(KeyboardKey(label = "⌫", hint = null, output = "", type = KeyType.BACKSPACE, weight = 1.25f))

        val row4 = listOf(
            KeyboardKey(label = "?123", output = "", type = KeyType.SWITCH_SYMBOLS, weight = 1.2f),
            KeyboardKey(label = "😊", output = "", type = KeyType.SWITCH_EMOJI, weight = 1.0f),
            KeyboardKey(label = ",", output = ",", hint = "।", shiftOutput = "।", weight = 0.9f),
            KeyboardKey(label = "◀  জাতীয়  ▶", output = " ", type = KeyType.SPACE, weight = 3.8f),
            KeyboardKey(label = "।", output = "।", hint = ".", shiftOutput = ".", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )

        return listOf(row1, row2, row3Keys, row4)
    }
}
