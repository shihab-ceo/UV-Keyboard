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
     * Converts phonetic English text into Bengali Unicode using Avro grammar rules.
     */
    fun phoneticTransliterate(input: String): String = AvroPhoneticEngine.transliterate(input)
}
