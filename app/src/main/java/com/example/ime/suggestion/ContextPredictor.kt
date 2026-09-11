package com.example.ime.suggestion

object ContextPredictor {

    // Common bigram associations: preceding word -> list of likely next words
    private val bigramMap = mapOf(
        // English bigrams
        "to" to listOf("school", "market", "work", "home", "the", "see", "go", "make"),
        "going" to listOf("to", "home", "there", "out"),
        "am" to listOf("going", "happy", "here", "good", "fine", "ready", "doing"),
        "i" to listOf("am", "have", "will", "can", "want", "think", "know", "like", "feel"),
        "how" to listOf("are", "is", "do", "can", "much", "many"),
        "are" to listOf("you", "they", "we", "doing", "going"),
        "you" to listOf("are", "can", "have", "know", "want", "do", "like"),
        "good" to listOf("morning", "night", "day", "afternoon", "job", "idea"),
        "thank" to listOf("you", "very", "god"),
        "thanks" to listOf("for", "a", "to", "you"),
        "please" to listOf("help", "give", "tell", "send", "check"),
        "play" to listOf("game", "music", "video", "football"),
        "android" to listOf("phone", "keyboard", "app", "device", "system"),
        "happy" to listOf("birthday", "to", "for", "day"),

        // Bengali bigrams
        "আমি" to listOf("ভালো", "আছি", "করবো", "যাবো", "বলছি", "বাংলা"),
        "তুমি" to listOf("কেমন", "আছো", "কোথায়", "কী", "কবে"),
        "আপনি" to listOf("কেমন", "আছেন", "কোথায়", "কী"),
        "কেমন" to listOf("আছো", "আছেন", "খবর", "লাগে"),
        "ধন্যবাদ" to listOf("আপনাকে", "তোমাকে", "অনেক", "ভাই"),
        "আজকে" to listOf("game", "খেলবো", "বৃষ্টি", "ছুটি", "দেখা"),
        "বাংলা" to listOf("কিবোর্ড", "typing", "ভাষা", "দেশ", "গান"),
        "অনেক" to listOf("ধন্যবাদ", "সুন্দর", "ভালো", "কঠিন", "সহজ"),
        "খুব" to listOf("ভালো", "সুন্দর", "আনন্দ", "কষ্ট"),
        "শুভ" to listOf("সকাল", "রাত্রি", "জন্মদিন", "কামনা"),
        "আমার" to listOf("দেশ", "বন্ধু", "ভাই", "ফোন", "নাম")
    )

    /**
     * Given the preceding word, returns contextual candidates.
     */
    fun getContextCandidates(precedingWord: String?): List<String> {
        if (precedingWord.isNullOrBlank()) return emptyList()
        val cleaned = precedingWord.trim().lowercase()
        return bigramMap[cleaned] ?: bigramMap[precedingWord.trim()] ?: emptyList()
    }
}
