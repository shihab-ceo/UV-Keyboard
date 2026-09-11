package com.example.ime

/**
 * Official Prabhat (প্রভাত) Keyboard Layout Character Mapping (Ekushey standard).
 *
 * Probhat is a fixed Unicode layout that pairs vowels with their vowel signs (kar)
 * and consonants with their aspirated/retroflex counterparts:
 * - Row 1 (Q-P): দ/ধ, ূ/ঊ, ী/ঈ, র/ড়, ট/ঠ, য/য়, ু/উ, ি/ই, ো/ও, প/ফ
 * - Row 2 (A-;): া/অ (hint আ), স/ষ, ড/ঢ, ত/থ, গ/ঘ, হ/ঃ, জ/ঝ, ক/খ, ল/ং, ৃ/ঋ
 * - Row 3 (Z-M): ে/এ, ৈ/ঐ, ৌ/ঔ, চ/ছ, ব/ভ, ্/ৎ (hint ঁ), ন/ণ, ম/শ
 */
object PrabhatLayoutMap {

    fun getRows(): List<List<KeyboardKey>> {
        // Row 1 (10 keys):
        // Q: দ (ধ)
        // W: ূ (ঊ)
        // E: ী (ঈ)
        // R: র (ড়)
        // T: ট (ঠ)
        // Y: য (য়)
        // U: ু (উ)
        // I: ি (ই)
        // O: ো (ও)
        // P: প (ফ)
        val row1Mapping = listOf(
            Triple("দ", "ধ", "ধ"),
            Triple("ূ", "ঊ", "ঊ"),
            Triple("ী", "ঈ", "ঈ"),
            Triple("র", "ড়", "ড়"),
            Triple("ট", "ঠ", "ঠ"),
            Triple("য", "য়", "য়"),
            Triple("ু", "উ", "উ"),
            Triple("ি", "ই", "ই"),
            Triple("ো", "ও", "ও"),
            Triple("প", "ফ", "ফ")
        )
        val row1 = row1Mapping.map { (char, hint, shift) ->
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = shift)
        }

        // Row 2 (10 keys):
        // A: া (hint: আ, shift: অ)
        // S: স (ষ)
        // D: ড (ঢ)
        // F: ত (থ)
        // G: গ (ঘ)
        // H: হ (ঃ)
        // J: জ (ঝ)
        // K: ক (খ)
        // L: ল (ং)
        // ;: ৃ (ঋ)
        val row2Mapping = listOf(
            Triple("া", "আ", "অ"),
            Triple("স", "ষ", "ষ"),
            Triple("ড", "ঢ", "ঢ"),
            Triple("ত", "থ", "থ"),
            Triple("গ", "ঘ", "ঘ"),
            Triple("হ", "ঃ", "ঃ"),
            Triple("জ", "ঝ", "ঝ"),
            Triple("ক", "খ", "খ"),
            Triple("ল", "ং", "ং"),
            Triple("ৃ", "ঋ", "ঋ")
        )
        val row2 = row2Mapping.map { (char, hint, shift) ->
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = shift)
        }

        // Row 3 Keys (Shift + 8 Character Keys + Backspace = 10 keys):
        // Key 1: Shift ("⬆")
        // Key 2 (Z): ে (এ)
        // Key 3 (X): ৈ (ঐ)
        // Key 4 (C): ৌ (ঔ)
        // Key 5 (V): চ (ছ)
        // Key 6 (B): ব (ভ)
        // Key 7 (N): ্ (hint: ঁ, shift: ৎ)
        // Key 8 (M): ন (ণ)
        // Key 9 (,): ম (শ)
        // Key 10: Backspace ("⌫")
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(KeyboardKey(label = "⬆", hint = null, output = "", type = KeyType.SHIFT, weight = 1.25f))
        val row3Mapping = listOf(
            Triple("ে", "এ", "এ"),
            Triple("ৈ", "ঐ", "ঐ"),
            Triple("ৌ", "ঔ", "ঔ"),
            Triple("চ", "ছ", "ছ"),
            Triple("ব", "ভ", "ভ"),
            Triple("্", "ঁ", "ৎ"),
            Triple("ন", "ণ", "ণ"),
            Triple("ম", "শ", "শ")
        )
        row3Mapping.forEach { (char, hint, shift) ->
            row3Keys.add(KeyboardKey(label = char, hint = hint, output = char, shiftOutput = shift))
        }
        row3Keys.add(KeyboardKey(label = "⌫", hint = null, output = "", type = KeyType.BACKSPACE, weight = 1.25f))

        val row4 = listOf(
            KeyboardKey(label = "?123", output = "", type = KeyType.SWITCH_SYMBOLS, weight = 1.2f),
            KeyboardKey(label = "😊", output = "", type = KeyType.SWITCH_EMOJI, weight = 1.0f),
            KeyboardKey(label = ",", output = ",", hint = "।", shiftOutput = "।", weight = 0.9f),
            KeyboardKey(label = "◀  Prabhat  ▶", output = " ", type = KeyType.SPACE, weight = 3.8f),
            KeyboardKey(label = "।", output = "।", hint = ".", shiftOutput = ".", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )

        return listOf(row1, row2, row3Keys, row4)
    }
}
