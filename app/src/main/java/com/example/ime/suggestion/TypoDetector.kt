package com.example.ime.suggestion

import kotlin.math.abs
import kotlin.math.min

object TypoDetector {

    // Common adjacent QWERTY keys for proximity weighting
    private val qwertyNeighbors = mapOf(
        'q' to "waas",
        'w' to "qeas",
        'e' to "wrds",
        'r' to "etfd",
        't' to "rygf",
        'y' to "tuhg",
        'u' to "yijh",
        'i' to "uokj",
        'o' to "iplk",
        'p' to "ol",
        'a' to "qwsz",
        's' to "awedxz",
        'd' to "serfcx",
        'f' to "drtgvc",
        'g' to "ftyhbv",
        'h' to "gyujnb",
        'j' to "huikmn",
        'k' to "jiolm",
        'l' to "kop",
        'z' to "asx",
        'x' to "zsdc",
        'c' to "xdfv",
        'v' to "cfgb",
        'b' to "vghn",
        'n' to "bhjm",
        'm' to "njk"
    )

    /**
     * Calculates Damerau-Levenshtein distance with a hard cutoff (maxDistance).
     * Returns distance if <= maxDistance, otherwise Int.MAX_VALUE.
     */
    fun boundedDamerauLevenshtein(s1: String, s2: String, maxDistance: Int = 2): Int {
        val len1 = s1.length
        val len2 = s2.length

        if (abs(len1 - len2) > maxDistance) return Int.MAX_VALUE
        if (s1 == s2) return 0
        if (len1 == 0) return if (len2 <= maxDistance) len2 else Int.MAX_VALUE
        if (len2 == 0) return if (len1 <= maxDistance) len1 else Int.MAX_VALUE

        // 2D DP matrix
        val d = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) d[i][0] = i
        for (j in 0..len2) d[0][j] = j

        for (i in 1..len1) {
            val ch1 = s1[i - 1]
            var minInRow = d[i][0]

            for (j in 1..len2) {
                val ch2 = s2[j - 1]
                val cost = if (ch1 == ch2) 0 else 1

                var dist = min(
                    min(
                        d[i - 1][j] + 1,      // Deletion
                        d[i][j - 1] + 1       // Insertion
                    ),
                    d[i - 1][j - 1] + cost    // Substitution
                )

                // Transposition check (swap adjacent characters: "teh" -> "the")
                if (i > 1 && j > 1 && ch1 == s2[j - 2] && s1[i - 2] == ch2) {
                    dist = min(dist, d[i - 2][j - 2] + 1)
                }

                d[i][j] = dist
                minInRow = min(minInRow, dist)
            }

            if (minInRow > maxDistance) {
                return Int.MAX_VALUE
            }
        }

        val result = d[len1][len2]
        return if (result <= maxDistance) result else Int.MAX_VALUE
    }

    /**
     * Checks if two characters are keyboard neighbors on standard layout.
     */
    fun isKeyboardNeighbor(c1: Char, c2: Char): Boolean {
        val neighbors = qwertyNeighbors[c1.lowercaseChar()] ?: return false
        return neighbors.contains(c2.lowercaseChar())
    }

    /**
     * Collapse repeated characters: e.g. "helllllo" -> "hello", "বাাংলা" -> "বাংলা"
     */
    fun collapseRepeatedCharacters(input: String): String {
        if (input.length <= 2) return input
        val sb = StringBuilder()
        var repeatCount = 0
        var prevChar = '\u0000'

        for (c in input) {
            if (c == prevChar) {
                repeatCount++
                if (repeatCount <= 2) {
                    sb.append(c)
                }
            } else {
                repeatCount = 1
                prevChar = c
                sb.append(c)
            }
        }
        return sb.toString()
    }
}
