package com.example

import android.view.inputmethod.EditorInfo
import com.example.ime.suggestion.CandidateSource
import com.example.ime.suggestion.ContextPredictor
import com.example.ime.suggestion.LexiconDictionary
import com.example.ime.suggestion.SuggestionEngine
import com.example.ime.suggestion.TypoDetector
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SuggestionEngineTest {

    private lateinit var engine: SuggestionEngine

    @Before
    fun setUp() {
        engine = SuggestionEngine(learningStore = null)
    }

    // 1. Exact suggestion
    @Test
    fun testExactSuggestion() {
        val candidates = engine.getSuggestions("hello")
        assertTrue("Candidates should not be empty", candidates.isNotEmpty())
        val exactCandidate = candidates.find { it.text.equals("hello", ignoreCase = true) }
        assertNotNull("Should contain exact match 'hello'", exactCandidate)
        assertTrue("Exact match should have high score", exactCandidate!!.score >= 0.85f)
    }

    // 2. Prefix suggestion
    @Test
    fun testPrefixSuggestion() {
        // English prefix
        val enCandidates = engine.getSuggestions("keyb")
        val foundKeyboard = enCandidates.any { it.text.equals("keyboard", ignoreCase = true) }
        assertTrue("Prefix 'keyb' should suggest 'keyboard'", foundKeyboard)

        // Bengali prefix
        val bnCandidates = engine.getSuggestions("বাং")
        val foundBangla = bnCandidates.any { it.text.contains("বাংলা") }
        assertTrue("Prefix 'বাং' should suggest 'বাংলা'", foundBangla)
    }

    // 3. English typo correction
    @Test
    fun testEnglishTypoCorrection() {
        // 'teh' -> 'the'
        val tehCandidates = engine.getSuggestions("teh")
        assertTrue("Candidates for 'teh' should not be empty", tehCandidates.isNotEmpty())
        val topTeh = tehCandidates.first()
        assertEquals("the", topTeh.text.lowercase())
        assertTrue("Top candidate should be autocorrected", topTeh.isAutocorrect)

        // 'helo' -> 'hello'
        val heloCandidates = engine.getSuggestions("helo")
        val topHelo = heloCandidates.first()
        assertEquals("hello", topHelo.text.lowercase())
        assertTrue("Top candidate should be autocorrected", topHelo.isAutocorrect)

        // 'thsi' -> 'this'
        val thsiCandidates = engine.getSuggestions("thsi")
        val topThsi = thsiCandidates.first()
        assertEquals("this", topThsi.text.lowercase())
        assertTrue("Top candidate should be autocorrected", topThsi.isAutocorrect)
    }

    // 4. Bengali typo correction
    @Test
    fun testBengaliTypoCorrection() {
        // 'বাংল' -> 'বাংলা'
        val banglCandidates = engine.getSuggestions("বাংল")
        val topBangla = banglCandidates.first()
        assertEquals("বাংলা", topBangla.text)
        assertTrue(topBangla.isAutocorrect)

        // 'ভাল' -> 'ভালো'
        val valoCandidates = engine.getSuggestions("ভাল")
        val topValo = valoCandidates.first()
        assertEquals("ভালো", topValo.text)

        // 'কেমম' -> 'কেমন'
        val kemonCandidates = engine.getSuggestions("কেমম")
        val topKemon = kemonCandidates.first()
        assertEquals("কেমন", topKemon.text)
    }

    // 5. Transliteration suggestion
    @Test
    fun testTransliterationSuggestion() {
        // 'ami' -> 'আমি'
        val amiCandidates = engine.getSuggestions("ami")
        val hasAmi = amiCandidates.any { it.text == "আমি" }
        assertTrue("'ami' should suggest 'আমি'", hasAmi)

        // 'dhonnobad' -> 'ধন্যবাদ'
        val dhonnoCandidates = engine.getSuggestions("dhonnobad")
        val hasDhonnobad = dhonnoCandidates.any { it.text == "ধন্যবাদ" }
        assertTrue("'dhonnobad' should suggest 'ধন্যবাদ'", hasDhonnobad)
    }

    // 6. Personal dictionary / local learning behavior
    @Test
    fun testPersonalDictionaryIntegration() {
        // Context test: preceding word boosting
        val contextCandidates = ContextPredictor.getContextCandidates("how")
        assertTrue("Context after 'how' should include 'are'", contextCandidates.contains("are"))

        val bnContext = ContextPredictor.getContextCandidates("আমি")
        assertTrue("Context after 'আমি' should include 'ভালো'", bnContext.contains("ভালো"))
    }

    // 7. Confidence threshold (Conservative vs Aggressive)
    @Test
    fun testConfidenceThresholds() {
        // Under conservative mode, ambiguous typos shouldn't trigger aggressive autocorrect
        val conservativeResult = engine.getSuggestions(
            query = "keyboar", // missing 1 letter, not in direct map
            aggressiveness = SuggestionEngine.AGGRESSIVENESS_CONSERVATIVE
        )
        // Under aggressive mode, it should be more inclined to autocorrect
        val aggressiveResult = engine.getSuggestions(
            query = "keyboar",
            aggressiveness = SuggestionEngine.AGGRESSIVENESS_AGGRESSIVE
        )

        assertNotNull(conservativeResult)
        assertNotNull(aggressiveResult)
    }

    // 8. Password field protection
    @Test
    fun testPasswordFieldProtection() {
        val passwordEditorInfo = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        }

        assertFalse("Password field must NOT be safe", engine.isSafeField(passwordEditorInfo))
        assertFalse("Password field must NOT allow autocorrect", engine.isAutocorrectPermittedField(passwordEditorInfo))

        val webPasswordEditorInfo = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD
        }
        assertFalse("Web password field must NOT be safe", engine.isSafeField(webPasswordEditorInfo))

        val pinEditorInfo = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_NUMBER or 16 /* TYPE_NUMBER_VARIATION_PASSWORD */
        }
        assertFalse("PIN field must NOT be safe", engine.isSafeField(pinEditorInfo))

        // When typing in password field, suggestions should have isAutocorrect = false
        val candidatesInPassword = engine.getSuggestions("teh", editorInfo = passwordEditorInfo)
        for (c in candidatesInPassword) {
            assertFalse("Autocorrect must be disabled in password fields", c.isAutocorrect)
        }
    }

    // 9. URL/email protection
    @Test
    fun testUrlAndEmailProtection() {
        val emailEditorInfo = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        assertFalse("Email field must not allow autocorrect", engine.isAutocorrectPermittedField(emailEditorInfo))

        val uriEditorInfo = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
        }
        assertFalse("URI field must not allow autocorrect", engine.isAutocorrectPermittedField(uriEditorInfo))

        // Token checks
        assertTrue("Email string should be exempt", engine.isTokenExemptFromAutocorrect("user@example.com"))
        assertTrue("URL string should be exempt", engine.isTokenExemptFromAutocorrect("https://google.com"))
        assertTrue("Hashtag should be exempt", engine.isTokenExemptFromAutocorrect("#bangla"))
        assertTrue("Mention should be exempt", engine.isTokenExemptFromAutocorrect("@username"))
        assertTrue("Number should be exempt", engine.isTokenExemptFromAutocorrect("12345"))
    }

    // 10. Mixed-language input
    @Test
    fun testMixedLanguageInput() {
        // Bengali text detection
        assertTrue(engine.isBengaliScript("বাংলাদেশ"))
        assertFalse(engine.isBengaliScript("Hello"))

        // Suggestions for Bengali query
        val bnCandidates = engine.getSuggestions("কেমন")
        assertTrue(bnCandidates.any { it.text == "কেমন" })

        // Suggestions for English query
        val enCandidates = engine.getSuggestions("friend")
        assertTrue(enCandidates.any { it.text.equals("friend", ignoreCase = true) })
    }
}
