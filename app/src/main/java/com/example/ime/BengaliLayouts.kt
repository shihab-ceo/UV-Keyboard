package com.example.ime

object BengaliLayouts {

    fun getJatiyaRows(): List<List<KeyboardKey>> = JatiyaLayoutMap.getRows()

    fun getPrabhatRows(): List<List<KeyboardKey>> {
        val row1Hints = listOf("এ", "ড়", "ঠ", "থ", "ূ", "ী", "ও", "ফ", "ঘ", "ঢ়")
        val row1Main  = listOf("ে", "র", "ট", "ত", "ু", "ি", "ো", "প", "গ", "দ")
        val row1 = row1Main.mapIndexed { idx, char ->
            val hint = row1Hints.getOrNull(idx)
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = hint ?: char)
        }

        val row2Hints = listOf("অ", "ষ", "ঢ", "ধ", "খ", "ঃ", "ঝ", "ছ", "ল")
        val row2Main  = listOf("া", "স", "ড", "্", "ক", "হ", "জ", "চ", "ব")
        val row2 = row2Main.mapIndexed { idx, char ->
            val hint = row2Hints.getOrNull(idx)
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = hint ?: char)
        }

        val row3Hints = listOf("ং", "য়", "শ", "ভ", "ণ", "ম")
        val row3Main  = listOf("য", "ব", "ন", "ম", "ল", "হ")
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
            KeyboardKey(label = "◀  Prabhat  ▶", output = " ", type = KeyType.SPACE, weight = 3.8f),
            KeyboardKey(label = "।", output = "।", hint = ".", shiftOutput = ".", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )
        return listOf(row1, row2, row3Keys, row4)
    }

    fun getEnglishRows(): List<List<KeyboardKey>> {
        val row1Hints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val row1Main  = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row1 = row1Main.mapIndexed { idx, char ->
            val hint = row1Hints.getOrNull(idx)
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = char.uppercase())
        }

        val row2Hints = listOf("@", "#", "&", "*", "-", "!", "?", "(", ")")
        val row2Main  = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row2 = row2Main.mapIndexed { idx, char ->
            val hint = row2Hints.getOrNull(idx)
            KeyboardKey(label = char, hint = hint, output = char, shiftOutput = char.uppercase())
        }

        val row3Hints = listOf("\"", "'", ":", "/", ";", "<", ">")
        val row3Main  = listOf("z", "x", "c", "v", "b", "n", "m")
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(KeyboardKey(label = "⬆", hint = null, output = "", type = KeyType.SHIFT, weight = 1.3f))
        row3Main.forEachIndexed { idx, char ->
            val hint = row3Hints.getOrNull(idx)
            row3Keys.add(KeyboardKey(label = char, hint = hint, output = char, shiftOutput = char.uppercase()))
        }
        row3Keys.add(KeyboardKey(label = "⌫", hint = null, output = "", type = KeyType.BACKSPACE, weight = 1.3f))

        val row4 = listOf(
            KeyboardKey(label = "?123", output = "", type = KeyType.SWITCH_SYMBOLS, weight = 1.2f),
            KeyboardKey(label = "😊", output = "", type = KeyType.SWITCH_EMOJI, weight = 1.0f),
            KeyboardKey(label = ",", output = ",", weight = 0.9f),
            KeyboardKey(label = "◀  English  ▶", output = " ", type = KeyType.SPACE, weight = 3.8f),
            KeyboardKey(label = ".", output = ".", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )

        return listOf(row1, row2, row3Keys, row4)
    }

    fun getAvroRows(): List<List<KeyboardKey>> {
        val row1Hints = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০")
        val row1Main  = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row1 = row1Main.mapIndexed { idx, char ->
            KeyboardKey(label = char, hint = row1Hints.getOrNull(idx), output = char, shiftOutput = char.uppercase())
        }

        val row2Hints = listOf("@", "#", "&", "*", "-", "!", "?", "(", ")")
        val row2Main  = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row2 = row2Main.mapIndexed { idx, char ->
            KeyboardKey(label = char, hint = row2Hints.getOrNull(idx), output = char, shiftOutput = char.uppercase())
        }

        val row3Main = listOf("z", "x", "c", "v", "b", "n", "m")
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(KeyboardKey(label = "⬆", hint = null, output = "", type = KeyType.SHIFT, weight = 1.3f))
        row3Main.forEach { char ->
            row3Keys.add(KeyboardKey(label = char, hint = null, output = char, shiftOutput = char.uppercase()))
        }
        row3Keys.add(KeyboardKey(label = "⌫", hint = null, output = "", type = KeyType.BACKSPACE, weight = 1.3f))

        val row4 = listOf(
            KeyboardKey(label = "?123", output = "", type = KeyType.SWITCH_SYMBOLS, weight = 1.2f),
            KeyboardKey(label = "😊", output = "", type = KeyType.SWITCH_EMOJI, weight = 1.0f),
            KeyboardKey(label = ",", output = ",", weight = 0.9f),
            KeyboardKey(label = "◀  Avro ফোনেটিক  ▶", output = " ", type = KeyType.SPACE, weight = 3.8f),
            KeyboardKey(label = "।", output = "।", hint = ".", shiftOutput = ".", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )

        return listOf(row1, row2, row3Keys, row4)
    }

    fun getBijoyRows(): List<List<KeyboardKey>> = BijoyLayoutMap.getRows()

    fun getSymbolRows(): List<List<KeyboardKey>> {
        val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
            KeyboardKey(label = it, output = it)
        }
        val row2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "৳").map {
            KeyboardKey(label = it, output = it)
        }
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(KeyboardKey(label = "১২৩", output = "", type = KeyType.SHIFT, weight = 1.3f))
        listOf("*", "\"", "'", ":", ";", "!", "?", "/").forEach {
            row3Keys.add(KeyboardKey(label = it, output = it))
        }
        row3Keys.add(KeyboardKey(label = "⌫", output = "", type = KeyType.BACKSPACE, weight = 1.3f))

        val row4 = listOf(
            KeyboardKey(label = "ABC", output = "", type = KeyType.SWITCH_SYMBOLS, weight = 1.3f),
            KeyboardKey(label = "😊", output = "", type = KeyType.SWITCH_EMOJI, weight = 1.0f),
            KeyboardKey(label = ",", output = ",", weight = 0.9f),
            KeyboardKey(label = "Space", output = " ", type = KeyType.SPACE, weight = 3.6f),
            KeyboardKey(label = ".", output = ".", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )
        return listOf(row1, row2, row3Keys, row4)
    }

    fun getBengaliNumberRows(): List<List<KeyboardKey>> {
        val row1 = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০").map {
            KeyboardKey(label = it, output = it)
        }
        val row2 = listOf("৳", "।", "ঃ", "ঁ", "্", "=", "<", ">", "{", "}").map {
            KeyboardKey(label = it, output = it)
        }
        val row3Keys = mutableListOf<KeyboardKey>()
        row3Keys.add(KeyboardKey(label = "123", output = "", type = KeyType.SHIFT, weight = 1.3f))
        listOf("[", "]", "\\", "|", "~", "^", "_", "`").forEach {
            row3Keys.add(KeyboardKey(label = it, output = it))
        }
        row3Keys.add(KeyboardKey(label = "⌫", output = "", type = KeyType.BACKSPACE, weight = 1.3f))

        val row4 = listOf(
            KeyboardKey(label = "ABC", output = "", type = KeyType.SWITCH_SYMBOLS, weight = 1.3f),
            KeyboardKey(label = "😊", output = "", type = KeyType.SWITCH_EMOJI, weight = 1.0f),
            KeyboardKey(label = ",", output = ",", weight = 0.9f),
            KeyboardKey(label = "Space", output = " ", type = KeyType.SPACE, weight = 3.6f),
            KeyboardKey(label = "।", output = "।", weight = 0.9f),
            KeyboardKey(label = "🔍", output = "\n", type = KeyType.ENTER, weight = 1.3f)
        )
        return listOf(row1, row2, row3Keys, row4)
    }

    val EMOJIS = listOf(
        "😊", "😂", "❤️", "👍", "🙏", "😍", "👏", "🔥", "🎉", "😎",
        "🥳", "🥺", "😢", "🤩", "✨", "✌️", "💯", "🌸", "🇧🇩", "🤲",
        "🤝", "💡", "📌", "🎯", "☕", "🌺", "🌹", "⚡", "🌟", "💪"
    )
}
