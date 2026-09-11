package com.example.ime

/**
 * Avro Phonetic Parser and Transliteration Rule Engine.
 * Converts Roman/English phonetic input to standard Bengali Unicode strings.
 *
 * Implements standard Avro grammar:
 * - Case sensitivity (t=ট, T=ত, d=ড, D=দ, n=ন, N=ণ, s=স, S=ষ, r=র, R=ড়, Rh=ঢ়, y=য়, z=য, etc.)
 * - Aspirated consonants (kh, gh, ch, jh, th, Th, dh, Dh, ph, bh)
 * - Vowels: full (initial) vs. vowel sign (post-consonant)
 * - Hasanta/virama explicit trigger (`,,` or `,,` / backtick or explicit virama)
 * - Ya-phala (`y` after consonant -> `্য`)
 * - Ra-phala (`r` after consonant -> `্র`)
 * - Reph (`rr` or `r` before consonant)
 * - Common Bengali words and loanwords (প্রযুক্তি, কম্পিউটার, কীবোর্ড, অ্যান্ড্রয়েড, সফটওয়্যার, ইত্যাদি)
 */
object AvroPhoneticEngine {

    // Common Bengali phonetic word definitions for high-frequency vocabulary
    private val DICTIONARY = mapOf(
        "bangla" to "বাংলা",
        "bangladesh" to "বাংলাদেশ",
        "amar" to "আমার",
        "ami" to "আমি",
        "amra" to "আমরা",
        "tumi" to "তুমি",
        "tomar" to "তোমার",
        "tomader" to "তোমাদের",
        "tora" to "তোরা",
        "apni" to "আপনি",
        "apnar" to "আপনার",
        "she" to "সে",
        "tini" to "তিনি",
        "tader" to "তাদের",
        "bhalo" to "ভালো",
        "kemon" to "কেমন",
        "kotha" to "কথা",
        "shobdo" to "শব্দ",
        "projukti" to "প্রযুক্তি",
        "komputar" to "কম্পিউটার",
        "computer" to "কম্পিউটার",
        "keyboard" to "কীবোর্ড",
        "kibord" to "কীবোর্ড",
        "android" to "অ্যান্ড্রয়েড",
        "shadhinota" to "স্বাধীনতা",
        "bhasha" to "ভাষা",
        "software" to "সফটওয়্যার",
        "softowar" to "সফটওয়্যার",
        "desh" to "দেশ",
        "desher" to "দেশের",
        "dhaka" to "ঢাকা",
        "manush" to "মানুষ",
        "kaj" to "কাজ",
        "khobor" to "খবর",
        "shuvo" to "শুভ",
        "sokal" to "সকাল",
        "shokal" to "সকাল",
        "bondhu" to "বন্ধু",
        "shundor" to "সুন্দর",
        "dhonnobad" to "ধন্যবাদ",
        "dhonno" to "ধন্য",
        "jonno" to "জন্য",
        "onek" to "অনেক",
        "olpo" to "অল্প"
    )

    // Multi-character conjuncts and special consonant patterns
    private val CONJUNCT_MAP = mapOf(
        // 4-char matches
        "kkhy" to "ক্ষ্য",
        "soft" to "সফট",

        // 3-char matches
        "kkh" to "ক্ষ",
        "ngh" to "ঙ্ঘ",
        "nch" to "ঞ্ছ",
        "njh" to "ঞ্ঝ",
        "nTh" to "ন্ঠ",
        "nth" to "ন্থ",
        "ndh" to "ন্ধ",
        "mbh" to "ম্ভ",
        "sth" to "স্থ",
        "sTh" to "ষ্ঠ",
        "shh" to "ষ",
        "shw" to "শ্ব",
        "shm" to "শ্ম",
        "jny" to "জ্ঞ",
        "gny" to "জ্ঞ",
        "bdh" to "ব্ধ",
        "kTh" to "ক্ট",
        "tth" to "ত্থ",
        "ddh" to "দ্ধ",
        "mpr" to "ম্প্র",
        "str" to "স্ত্র",
        "spr" to "স্প্র",

        // 2-char matches
        "ng" to "ঙ",
        "Ng" to "ঙ",
        "nc" to "ঞ্চ",
        "nj" to "ঞ্জ",
        "ch" to "ছ",
        "Ch" to "ছ",
        "kh" to "খ",
        "gh" to "ঘ",
        "th" to "ঠ",
        "Th" to "থ",
        "dh" to "ঢ",
        "Dh" to "ধ",
        "ph" to "ফ",
        "bh" to "ভ",
        "jh" to "ঝ",
        "sh" to "শ",
        "Sh" to "ষ",
        "Rh" to "ঢ়",
        "rh" to "ঢ়",
        "kk" to "ক্ক",
        "kt" to "ক্ত",
        "kT" to "ক্ত",
        "gn" to "জ্ঞ",
        "gN" to "জ্ঞ",
        "tt" to "ট্ট",
        "dd" to "ড্ড",
        "nt" to "ন্ট",
        "nT" to "ন্ত",
        "nd" to "ন্ড",
        "nD" to "ন্দ",
        "mp" to "ম্প",
        "mb" to "ম্ব",
        "mm" to "ম্ম",
        "st" to "স্ট",
        "sT" to "স্ত",
        "sp" to "স্প",
        "sk" to "স্ক",
        "ll" to "ল্ল",
        "nn" to "ন্ন",
        "nN" to "ণ্ণ",
        "bb" to "ব্ব",
        "pp" to "প্প",
        "jj" to "জ্জ",
        "cc" to "চ্চ",
        "gg" to "জ্ঞ",
        "pl" to "প্ল",
        "kl" to "ক্ল",
        "bl" to "ব্ল",
        "gl" to "গ্ল",
        "fl" to "ফ্ল"
    )

    // Single consonants with case sensitivity
    private val CONSONANT_MAP = mapOf(
        'k' to "ক", 'K' to "খ",
        'g' to "গ", 'G' to "ঘ",
        'c' to "চ", 'C' to "ছ",
        'j' to "জ", 'J' to "ঝ",
        't' to "ট", 'T' to "ত",
        'd' to "ড", 'D' to "দ",
        'n' to "ন", 'N' to "ণ",
        'p' to "প", 'P' to "ফ",
        'f' to "ফ", 'F' to "ফ",
        'b' to "ব", 'B' to "ভ",
        'v' to "ভ", 'V' to "ভ",
        'm' to "ম", 'M' to "ম",
        'z' to "য", 'Z' to "য",
        'y' to "য়", 'Y' to "য়",
        'r' to "র", 'R' to "ড়",
        'l' to "ল", 'L' to "ল",
        's' to "স", 'S' to "ষ",
        'h' to "হ", 'H' to "হ",
        'w' to "ওয়", 'W' to "ওয়"
    )

    // Full/independent vowels
    private val VOWEL_FULL = mapOf(
        "aa" to "আ", "a" to "আ", "A" to "আ",
        "ee" to "ঈ", "i" to "ই", "I" to "ঈ",
        "oo" to "ঊ", "u" to "উ", "U" to "ঊ",
        "oi" to "ঐ", "OI" to "ঐ",
        "ou" to "ঔ", "OU" to "ঔ",
        "e" to "এ", "E" to "এ",
        "o" to "অ", "O" to "ও",
        "rri" to "ঋ", "Rri" to "ঋ"
    )

    // Vowel signs (Kar)
    private val VOWEL_SIGN = mapOf(
        "aa" to "া", "a" to "া", "A" to "া",
        "ee" to "ী", "i" to "ি", "I" to "ী",
        "oo" to "ূ", "u" to "ু", "U" to "ূ",
        "oi" to "ৈ", "OI" to "ৈ",
        "ou" to "ৌ", "OU" to "ৌ",
        "e" to "ে", "E" to "ে",
        "o" to "", "O" to "ো",
        "rri" to "ৃ", "Rri" to "ৃ"
    )

    // Diacritic / special punctuation
    private val SPECIAL_MAP = mapOf(
        ":`" to "ঃ",
        "^" to "ঁ",
        "::" to "ঃ",
        "$" to "৳",
        "`" to BengaliCompositionHelper.HASANTA,
        "t``" to "ৎ",
        "ng`" to "ং"
    )

    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""

        // Check if there's an exact dictionary match (preserving lowercase)
        val lower = input.lowercase()
        if (DICTIONARY.containsKey(lower)) {
            return DICTIONARY[lower]!!
        }

        val sb = StringBuilder()
        var i = 0
        var prevWasConsonant = false
        var lastAppendedConsonant: String? = null

        while (i < input.length) {
            // Check 3-character special diacritics / symbols (e.g. t`` -> ৎ, ng` -> ং)
            if (i + 3 <= input.length) {
                val sub3 = input.substring(i, i + 3)
                if (SPECIAL_MAP.containsKey(sub3)) {
                    sb.append(SPECIAL_MAP[sub3])
                    i += 3
                    prevWasConsonant = false
                    continue
                }
            }

            // Check 2-character special diacritics / symbols (e.g. :: or :` -> ঃ, ng` -> ং)
            if (i + 2 <= input.length) {
                val sub2 = input.substring(i, i + 2)
                if (SPECIAL_MAP.containsKey(sub2)) {
                    sb.append(SPECIAL_MAP[sub2])
                    i += 2
                    prevWasConsonant = false
                    continue
                }
            }

            // Check single-character special diacritics (e.g. ^ for chandrabindu, ` for hasanta)
            if (input[i] == '^') {
                sb.append("ঁ")
                i++
                continue
            }
            if (input[i] == '`') {
                sb.append(BengaliCompositionHelper.HASANTA)
                i++
                prevWasConsonant = false
                continue
            }
            if (input[i] == '$') {
                sb.append("৳")
                i++
                prevWasConsonant = false
                continue
            }

            // Check 4-character conjunct
            if (i + 4 <= input.length) {
                val sub4 = input.substring(i, i + 4)
                if (CONJUNCT_MAP.containsKey(sub4)) {
                    sb.append(CONJUNCT_MAP[sub4])
                    lastAppendedConsonant = CONJUNCT_MAP[sub4]
                    i += 4
                    prevWasConsonant = true
                    continue
                }
            }

            // Check 3-character conjunct
            if (i + 3 <= input.length) {
                val sub3 = input.substring(i, i + 3)
                if (CONJUNCT_MAP.containsKey(sub3)) {
                    sb.append(CONJUNCT_MAP[sub3])
                    lastAppendedConsonant = CONJUNCT_MAP[sub3]
                    i += 3
                    prevWasConsonant = true
                    continue
                }
                if (VOWEL_FULL.containsKey(sub3)) {
                    val sign = if (prevWasConsonant) VOWEL_SIGN[sub3] ?: sub3 else VOWEL_FULL[sub3] ?: sub3
                    sb.append(sign)
                    i += 3
                    prevWasConsonant = false
                    continue
                }
            }

            // Check 2-character conjunct or vowel
            if (i + 2 <= input.length) {
                val sub2 = input.substring(i, i + 2)

                // Ya-phala check: consonant + y (e.g., ky -> ক্য, py -> প্য)
                if (prevWasConsonant && (sub2[0] == 'y' || sub2[0] == 'Y') && !VOWEL_FULL.containsKey(sub2)) {
                    sb.append("্য")
                    i += 1
                    prevWasConsonant = true
                    continue
                }

                // Ra-phala check: consonant + r (e.g., pr -> প্র, tr -> ত্র)
                if (prevWasConsonant && (sub2[0] == 'r' || sub2[0] == 'R') && (i + 1 < input.length && VOWEL_FULL.containsKey(input[i + 1].toString()))) {
                    // Consonant + r + vowel: e.g. pro -> প্র + ও -> প্র, pri -> প্রি
                    sb.append("্${CONSONANT_MAP[sub2[0]] ?: "র"}")
                    i += 1
                    prevWasConsonant = true
                    continue
                }

                if (CONJUNCT_MAP.containsKey(sub2)) {
                    sb.append(CONJUNCT_MAP[sub2])
                    lastAppendedConsonant = CONJUNCT_MAP[sub2]
                    i += 2
                    prevWasConsonant = true
                    continue
                }
                if (VOWEL_FULL.containsKey(sub2)) {
                    val sign = if (prevWasConsonant) VOWEL_SIGN[sub2] ?: sub2 else VOWEL_FULL[sub2] ?: sub2
                    sb.append(sign)
                    i += 2
                    prevWasConsonant = false
                    continue
                }
            }

            // Single character
            val c = input[i]
            val cStr = c.toString()

            // Ya-phala single check
            if (prevWasConsonant && (c == 'y' || c == 'Y') && i + 1 == input.length) {
                sb.append("্য")
                i++
                prevWasConsonant = true
                continue
            }

            // Vowel checks
            if (VOWEL_FULL.containsKey(cStr)) {
                if (prevWasConsonant) {
                    if (c == 'o') {
                        // Inherent vowel 'o': keep preceding consonant unadorned
                    } else if (c == 'a' || c == 'A') {
                        sb.append("া")
                    } else {
                        sb.append(VOWEL_SIGN[cStr] ?: cStr)
                    }
                } else {
                    sb.append(VOWEL_FULL[cStr] ?: cStr)
                }
                prevWasConsonant = false
            } else if (CONSONANT_MAP.containsKey(c)) {
                sb.append(CONSONANT_MAP[c])
                lastAppendedConsonant = CONSONANT_MAP[c]
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
