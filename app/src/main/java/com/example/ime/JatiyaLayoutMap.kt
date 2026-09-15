package com.example.ime

/**
 * 100% ACCURATE Jatiya Layout (জাতীয়) Data Object and Mapping Specification.
 * Strictly adheres to standard Gboard/Redmi reference specifications:
 *
 * --- UNSHIFTED STATE (Normal View) ---
 * Row 1: [ ঙ ]  [ য ]  [ ড ]  [ প ]  [ ট ]  [ চ ]  [ জ ]  [ হ ]  [ গ ]  [ ড় ]
 * Row 2: [ ূ ]  [ ু ]  [ ি ]  [ ব ]  [ ্ ]  [ া ]  [ ক ]  [ ত ]  [ দ ]
 * Row 3: [ ৌ ]  [ ো ]  [ ে ]  [ র ]  [ ন ]  [ স ]  [ ম ]
 *
 * --- SHIFTED STATE (Shift Key Pressed or Long Press) ---
 * Row 1: [ ং ]  [ য় ]  [ ঢ ]  [ ফ ]  [ ঠ ]  [ ছ ]  [ ঝ ]  [ ঞ ]  [ ঘ ]  [ ঢ় ]
 * Row 2: [ ঋ ]  [ ঊ ]  [ ঈ ]  [ ভ ]  [ ্ ]  [ আ ]  [ খ ]  [ থ ]  [ ধ ]
 * Row 3: [ ঔ ]  [ ও ]  [ এ ]  [ ল ]  [ ণ ]  [ ষ ]  [ শ ]
 *
 * Key Design & Composition Rules:
 * 1. ZERO duplicate keys in Row 3: starts with 'ৌ' (Shift: 'ঔ') and ends with 'ম' (Shift: 'শ').
 * 2. Vowel signs (কার-চিহ্ন) are stripped of dotted circle (U+25CC / ◌) artifacts on labels.
 * 3. Exact 1-to-1 parity between unshifted character, shifted character, and output commits.
 */
object JatiyaLayoutMap {

    /**
     * Complete Jatiya Keyboard Rows matching standard Android IME specifications.
     */
    fun getRows(): List<List<KeyboardKey>> {
        // Row 1: 10 Character Keys
        val row1Main  = listOf("ঙ", "য", "ড", "প", "ট", "চ", "জ", "হ", "গ", "ড়")
        val row1Shift = listOf("ং", "য়", "ঢ", "ফ", "ঠ", "ছ", "ঝ", "ঞ", "ঘ", "ঢ়")
        val row1 = row1Main.mapIndexed { idx, char ->
            val shiftChar = row1Shift[idx]
            KeyboardKey(
                label = BengaliCompositionHelper.stripDottedCircle(char),
                hint = BengaliCompositionHelper.stripDottedCircle(shiftChar),
                output = char,
                shiftOutput = shiftChar
            )
        }

        // Row 2: 9 Character Keys
        val row2Main  = listOf("ূ", "ু", "ি", "ব", "্", "া", "ক", "ত", "দ")
        val row2Shift = listOf("ঋ", "ঊ", "ঈ", "ভ", "্", "আ", "খ", "থ", "ধ")
        val row2 = row2Main.mapIndexed { idx, char ->
            val shiftChar = row2Shift[idx]
            KeyboardKey(
                label = BengaliCompositionHelper.stripDottedCircle(char),
                hint = BengaliCompositionHelper.stripDottedCircle(shiftChar),
                output = char,
                shiftOutput = shiftChar
            )
        }

        // Row 3: Shift Key + 7 Character Keys + Backspace Key
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(
            KeyboardKey(
                label = "⬆",
                hint = null,
                output = "",
                type = KeyType.SHIFT,
                weight = 1.35f
            )
        )

        val row3Main  = listOf("ৌ", "ো", "ে", "র", "ন", "স", "ম")
        val row3Shift = listOf("ঔ", "ও", "এ", "ল", "ণ", "ষ", "শ")
        row3Main.forEachIndexed { idx, char ->
            val shiftChar = row3Shift[idx]
            row3Keys.add(
                KeyboardKey(
                    label = BengaliCompositionHelper.stripDottedCircle(char),
                    hint = BengaliCompositionHelper.stripDottedCircle(shiftChar),
                    output = char,
                    shiftOutput = shiftChar
                )
            )
        }

        row3Keys.add(
            KeyboardKey(
                label = "⌫",
                hint = null,
                output = "",
                type = KeyType.BACKSPACE,
                weight = 1.35f
            )
        )

        // Row 4: Action & Space Controls
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
