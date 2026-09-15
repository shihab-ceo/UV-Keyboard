package com.example

import android.view.inputmethod.InputConnection
import com.example.translator.TranslatorEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TranslatorEngineTest {

    private lateinit var engine: TranslatorEngine
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        engine = TranslatorEngine(ioDispatcher = testDispatcher)
    }

    @Test
    fun testOfflineFallbackBnToEn() = runTest(testDispatcher) {
        val translated = engine.translate("হ্যালো", "bn", "en")
        assertTrue("Should translate to Hello (case-insensitive)", translated.equals("Hello", ignoreCase = true))
    }

    @Test
    fun testOfflineFallbackEnToBn() = runTest(testDispatcher) {
        val translated = engine.translate("Hello", "en", "bn")
        assertTrue("Should translate to Bengali greeting", translated.contains("হ্যালো") || translated.contains("নমস্কার"))
    }

    @Test
    fun testSameSourceAndTargetReturnsOriginal() = runTest(testDispatcher) {
        val text = "Sample original text"
        val translated = engine.translate(text, "en", "en")
        assertEquals(text, translated)
    }

    @Test
    fun testEmptyInputReturnsEmpty() = runTest(testDispatcher) {
        val translated = engine.translate("   ", "bn", "en")
        assertEquals("", translated)
    }

    @Test
    fun testSafeCommitWithNullInputConnectionDoesNotCrash() {
        val result = TranslatorEngine.commitSafely(null, "Test", 0)
        assertFalse("Should return false when InputConnection is null", result)
    }

    @Test
    fun testSafeCommitWithZeroLengthAndEmptyText() {
        val mockConnection = object : android.view.inputmethod.BaseInputConnection(android.view.View(org.robolectric.RuntimeEnvironment.getApplication()), false) {}
        val result = TranslatorEngine.commitSafely(mockConnection, "", 0)
        assertTrue(result)
    }

    @Test
    fun testDebouncedTranslationWithOfflinePhrase() = testScope.runTest {
        var callbackResult: String? = null

        engine.translateDebounced(
            text = "ধন্যবাদ",
            sourceLang = "bn",
            targetLang = "en",
            scope = testScope
        ) { result ->
            callbackResult = result
        }

        // Before debounce delay
        assertNull("Callback should not fire immediately due to debounce delay", callbackResult)

        // Advance past debounce delay
        testScheduler.advanceTimeBy(350)
        advanceUntilIdle()

        assertTrue("Callback should translate 'ধন্যবাদ'", callbackResult?.contains("Thank", ignoreCase = true) == true)
    }

    @Test
    fun testCacheFunctionality() {
        val cacheKey = engine.buildCacheKey("bn", "en", "হ্যালো")
        assertEquals("bn->en:হ্যালো", cacheKey)

        engine.putInCache("টেস্ট", "bn", "en", "Test")
        val cached = engine.getCached("টেস্ট", "bn", "en")
        assertEquals("Test", cached)

        engine.clearCache()
        assertNull(engine.getCached("টেস্ট", "bn", "en"))
    }
}
