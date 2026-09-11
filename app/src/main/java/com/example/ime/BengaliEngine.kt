package com.example.ime

object BengaliEngine {

    private val commonWords = listOf(
        "জাতি", "জাতির", "জাতীয়", "জাতীয়তা",
        "বাংলাদেশ", "বাংলাদেশি", "বাঙালি", "বাংলা",
        "আমাদের", "আমার", "আমি", "আমরা",
        "তোমাদের", "তোমার", "তুমি", "তোরা",
        "তাদের", "তার", "তিনি", "তারা",
        "ধন্যবাদ", "স্বাগতম", "শুভেচ্ছা", "অভিনন্দন",
        "ভালোবাসা", "ভালোবাসি", "ভালো", "ভালোমন্দ",
        "কেমন", "আছেন", "আছি", "আছো",
        "সোনার", "মানুষ", "মানুষের", "জীবন",
        "স্বাধীনতা", "স্বাধীন", "মুক্তি", "যুদ্ধ",
        "বন্ধু", "বান্ধব", "বন্ধুত্ব", "সুন্দর",
        "কাজ", "কাজের", "খবর", "খবরের",
        "শুভ", "সকাল", "সন্ধ্যা", "রাত্রি", "রাত",
        "নমস্কার", "সালাম", "আসসালামু", "আলাইকুম",
        "ঠিক", "খুব", "অনেক", "অল্প",
        "সবাই", "সব", "আজ", "আজকে", "কাল", "কালকে",
        "এখন", "তখন", "কখন", "নতুন",
        "সময়", "সময়ের", "দেশ", "দেশের",
        "কথা", "কথাবার্তা", "বই", "গান", "গানবাজনা",
        "লেখা", "পড়া", "বিদ্যালয়", "কলেজ", "বিশ্ববিদ্যালয়",
        "সহায়তা", "সহজ", "সহজে", "দ্রুত",
        "ইউভি", "কীবোর্ড", "মোবাইল", "ফোন",
        "লেখালেখি", "বার্তা", "মেসেজ", "পোস্ট",
        "প্রথম", "দ্বিতীয়", "তৃতীয়", "শেষ",
        "খাবার", "পানি", "চাকরি", "ব্যবসা",
        "শহর", "গ্রাম", "নদী", "পাহাড়", "বৃষ্টি"
    )

    private val commonEnglishWords = listOf(
        "the", "and", "you", "that", "was", "for", "are", "with", "his", "they",
        "this", "have", "from", "one", "had", "word", "but", "not", "what", "all",
        "were", "when", "your", "can", "said", "there", "use", "each", "which", "she",
        "how", "their", "will", "other", "about", "out", "many", "then", "them", "these",
        "some", "her", "would", "make", "like", "him", "into", "time", "has", "look",
        "two", "more", "write", "see", "number", "way", "could", "people", "than",
        "first", "water", "been", "call", "who", "oil", "its", "now", "find", "long",
        "down", "day", "did", "get", "come", "made", "may", "part", "hello", "please",
        "thank", "thanks", "keyboard", "android", "bengali", "message", "mobile", "phone",
        "typing", "input", "service", "application", "good", "morning", "night", "today",
        "tomorrow", "yesterday", "friend", "love", "work", "home", "school", "college",
        "university", "bangladesh", "dhaka", "great", "nice", "fine", "welcome"
    )

    fun getPredictions(prefix: String, limit: Int = 5): List<String> {
        val trimmed = prefix.trim()
        if (trimmed.isEmpty()) {
            return listOf("জাতি", "জাতির", "জাতীয়", "বাংলাদেশ", "আমাদের")
        }
        val matches = commonWords.filter { it.startsWith(trimmed) }
        return if (matches.isNotEmpty()) {
            matches.take(limit)
        } else {
            val containing = commonWords.filter { it.contains(trimmed) }
            if (containing.isNotEmpty()) containing.take(limit)
            else listOf(trimmed)
        }
    }

    fun getEnglishPredictions(prefix: String, limit: Int = 5): List<String> {
        val trimmed = prefix.trim().lowercase()
        if (trimmed.isEmpty()) {
            return listOf("the", "and", "you", "that", "with")
        }
        val isUpper = prefix.isNotEmpty() && prefix[0].isUpperCase()
        val matches = commonEnglishWords.filter { it.startsWith(trimmed) }
        val results = if (matches.isNotEmpty()) {
            matches.take(limit)
        } else {
            val containing = commonEnglishWords.filter { it.contains(trimmed) }
            if (containing.isNotEmpty()) containing.take(limit)
            else listOf(trimmed)
        }
        return if (isUpper) results.map { it.replaceFirstChar { c -> c.uppercase() } } else results
    }

    fun transliterate(input: String): String = phoneticTransliterate(input)

    /**
     * Converts phonetic English text into Bengali Unicode.
     */
    fun phoneticTransliterate(input: String): String {
        if (input.isEmpty()) return ""

        // Common word shortcut override
        val lower = input.lowercase()
        val directMap = mapOf(
            "ami" to "আমি",
            "tumi" to "তুমি",
            "she" to "সে",
            "amra" to "আমরা",
            "bangla" to "বাংলা",
            "bangladesh" to "বাংলাদেশ",
            "amar" to "আমার",
            "shonar" to "সোনার",
            "dhaka" to "ঢাকা",
            "bhalobashi" to "ভালোবাসি",
            "kemon" to "কেমন",
            "achhen" to "আছেন",
            "achhi" to "আছি",
            "dhonnobad" to "ধন্যবাদ",
            "desh" to "দেশ",
            "desher" to "দেশের",
            "shadhin" to "স্বাধীন",
            "jonno" to "জন্য",
            "shob" to "সব",
            "kotha" to "কথা",
            "kaj" to "কাজ",
            "bhalo" to "ভালো",
            "manush" to "মানুষ",
            "jati" to "জাতি",
            "jatir" to "জাতির",
            "jatiyo" to "জাতীয়",
            "shun" to "শুন",
            "khobor" to "খবর",
            "shuvo" to "শুভ",
            "sokal" to "সকাল",
            "shokal" to "সকাল",
            "dhonno" to "ধন্য",
            "bondhu" to "বন্ধু",
            "shundor" to "সুন্দর"
        )
        if (directMap.containsKey(lower)) {
            return directMap[lower]!!
        }

        val conjunctMap = mapOf(
            "kkh" to "ক্ষ", "kkhy" to "ক্ষ্য",
            "ng" to "ঙ", "Ng" to "ঙ",
            "nj" to "ঞ্জ", "nc" to "ঞ্চ",
            "sh" to "শ", "Sh" to "ষ", "shh" to "ষ", "ch" to "ছ", "Ch" to "ছ",
            "jh" to "ঝ",
            "kh" to "খ", "gh" to "ঘ", "th" to "ঠ", "Th" to "থ",
            "dh" to "ঢ", "Dh" to "ধ", "ph" to "ফ", "bh" to "ভ",
            "Rh" to "ঢ়",
            "kk" to "ক্ক", "kt" to "ক্ত", "gn" to "জ্ঞ",
            "tt" to "ট্ট", "dd" to "ড্ড",
            "nt" to "ন্ত", "nd" to "ন্দ", "nth" to "ন্থ", "ndh" to "ন্ধ",
            "mp" to "ম্প", "mb" to "ম্ব", "mbh" to "ম্ভ", "mm" to "ম্ম",
            "st" to "স্ত", "sth" to "স্থ", "sp" to "স্প",
            "ll" to "ল্ল", "nn" to "ন্ন"
        )

        val consonantMap = mapOf(
            'k' to "ক", 'g' to "গ",
            'c' to "চ", 'j' to "জ",
            't' to "ট", 'T' to "ত",
            'd' to "ড", 'D' to "দ",
            'N' to "ণ", 'n' to "ন",
            'p' to "প", 'f' to "ফ", 'b' to "ব", 'v' to "ভ",
            'm' to "ম", 'z' to "য", 'y' to "য়",
            'r' to "র", 'l' to "ল",
            's' to "স", 'S' to "ষ", 'h' to "হ",
            'R' to "ড়", 'w' to "ও"
        )

        val vowelFull = mapOf(
            "aa" to "আ", "a" to "আ", "A" to "আ",
            "ee" to "ঈ", "i" to "ই", "I" to "ঈ",
            "oo" to "ঊ", "u" to "উ", "U" to "ঊ",
            "oi" to "ঐ", "e" to "এ", "E" to "এ",
            "ou" to "ঔ", "o" to "অ", "O" to "ও",
            "rri" to "ঋ"
        )

        val vowelSign = mapOf(
            "aa" to "া", "a" to "া", "A" to "া",
            "ee" to "ী", "i" to "ি", "I" to "ী",
            "oo" to "ূ", "u" to "ু", "U" to "ূ",
            "oi" to "ৈ", "e" to "ে", "E" to "ে",
            "ou" to "ৌ", "o" to "", "O" to "ো",
            "rri" to "ৃ"
        )

        val sb = StringBuilder()
        var i = 0
        var prevWasConsonant = false

        while (i < input.length) {
            // Check 3-char match
            if (i + 3 <= input.length) {
                val sub3 = input.substring(i, i + 3)
                if (conjunctMap.containsKey(sub3)) {
                    sb.append(conjunctMap[sub3])
                    i += 3
                    prevWasConsonant = true
                    continue
                }
                if (vowelFull.containsKey(sub3)) {
                    sb.append(if (prevWasConsonant) vowelSign[sub3] ?: sub3 else vowelFull[sub3])
                    i += 3
                    prevWasConsonant = false
                    continue
                }
            }

            // Check 2-char match
            if (i + 2 <= input.length) {
                val sub2 = input.substring(i, i + 2)
                if (conjunctMap.containsKey(sub2)) {
                    sb.append(conjunctMap[sub2])
                    i += 2
                    prevWasConsonant = true
                    continue
                }
                if (vowelFull.containsKey(sub2)) {
                    sb.append(if (prevWasConsonant) vowelSign[sub2] ?: sub2 else vowelFull[sub2])
                    i += 2
                    prevWasConsonant = false
                    continue
                }
            }

            // Single char
            val c = input[i]
            val cStr = c.toString()

            if (vowelFull.containsKey(cStr)) {
                if (prevWasConsonant) {
                    if (c == 'o') {
                        // Inherent vowel 'o' keeps preceding consonant plain without kar
                    } else if (c == 'a') {
                        sb.append("া")
                    } else {
                        sb.append(vowelSign[cStr] ?: cStr)
                    }
                } else {
                    sb.append(vowelFull[cStr] ?: cStr)
                }
                prevWasConsonant = false
            } else if (consonantMap.containsKey(c)) {
                sb.append(consonantMap[c])
                prevWasConsonant = true
            } else {
                sb.append(c)
                prevWasConsonant = false
            }
            i++
        }

        return sb.toString()
    }
}
