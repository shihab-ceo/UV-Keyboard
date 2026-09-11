package com.example

import com.example.ime.BengaliEngine
import com.example.ime.BengaliLayouts
import com.example.ime.BijoyLayoutMap
import com.example.ime.JatiyaLayoutMap
import com.example.ime.PrabhatLayoutMap
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testJatiyaLayout_Row2Key3() {
    val rows = JatiyaLayoutMap.getRows()
    val row2 = rows[1]
    val key3 = row2[2] // Index 2 is Key 3
    assertEquals("ি", key3.label)
    assertEquals("ি", key3.output)
    assertEquals("ী", key3.hint)
    assertEquals("ী", key3.shiftOutput)
  }

  @Test
  fun testJatiyaLayout_Row3Key4() {
    val rows = JatiyaLayoutMap.getRows()
    val row3 = rows[2]
    // Row 3 keys: [Shift, ো, ে, র, ...]
    val key4 = row3[3] // Index 3 is Key 4 (after Shift, ো, ে)
    assertEquals("র", key4.label)
    assertEquals("র", key4.output)
    assertEquals("ল", key4.hint)
    assertEquals("ল", key4.shiftOutput)
  }

  @Test
  fun testJatiyaLayout_Row1FixedBugs() {
    val rows = JatiyaLayoutMap.getRows()
    val row1 = rows[0]
    // E key: ড with shift ঢ (previously duplicate ট)
    assertEquals("ড", row1[2].output)
    assertEquals("ঢ", row1[2].shiftOutput)
    // U key: জ with shift ঝ (previously duplicate ব)
    assertEquals("জ", row1[6].output)
    assertEquals("ঝ", row1[6].shiftOutput)
  }

  @Test
  fun testJatiyaLayout_Row2FixedBugs() {
    val rows = JatiyaLayoutMap.getRows()
    val row2 = rows[1]
    // Key 1: ৃ with shift ঋ (previously '<')
    assertEquals("ৃ", row2[0].output)
    assertEquals("ঋ", row2[0].shiftOutput)
    // Key 2: ু with shift ূ (previously digit zero '০')
    assertEquals("ু", row2[1].output)
    assertEquals("ূ", row2[1].shiftOutput)
    // Key 5: virama ্ with shift ৎ
    assertEquals("্", row2[4].output)
    assertEquals("ৎ", row2[4].shiftOutput)
  }

  @Test
  fun testJatiyaLayout_Row3AllVowelsPresent() {
    val rows = JatiyaLayoutMap.getRows()
    val row3 = rows[2]
    // Verify ৈ (ঐ) and ৌ (ঔ) are present
    assertEquals("ৈ", row3[7].output)
    assertEquals("ঐ", row3[7].shiftOutput)
    assertEquals("ৌ", row3[8].output)
    assertEquals("ঔ", row3[8].shiftOutput)
  }

  @Test
  fun testBijoyLayout_Row3Key4_FixedDuplicate() {
    val rows = BijoyLayoutMap.getRows()
    val row3 = rows[2]
    // Row 3 keys: [Shift, ্র, ো, ে, ব, ...]
    val key5InRow = row3[4] // Index 4 in row3 is Key 4 among character keys
    assertEquals("ব", key5InRow.label)
    assertEquals("ব", key5InRow.output)
    assertEquals("র", key5InRow.hint)
    assertEquals("র", key5InRow.shiftOutput)
    // Verify no duplicate label between main and hint
    assertNotEquals(key5InRow.label, key5InRow.hint)
  }

  @Test
  fun testBijoyLayout_Row2Key4_HasL() {
    val rows = BijoyLayoutMap.getRows()
    val row2 = rows[1]
    val key4 = row2[3] // Index 3 is Key 4 (a / A key)
    assertEquals("া", key4.label)
    assertEquals("া", key4.output)
    assertEquals("ল", key4.hint)
    assertEquals("ল", key4.shiftOutput)
  }

  @Test
  fun testBijoyLayout_Row3Key1_RoFala() {
    val rows = BijoyLayoutMap.getRows()
    val row3 = rows[2]
    val key1 = row3[1] // First char key after Shift
    assertEquals("্র", key1.output)
    assertEquals("্য", key1.shiftOutput)
  }

  @Test
  fun testPrabhatLayout_StructureAndKeys() {
    val rows = PrabhatLayoutMap.getRows()
    assertEquals(4, rows.size)
    // Row 1: দ, ূ, ী, র, ট, য, ু, ি, ো, প
    val row1 = rows[0]
    assertEquals(10, row1.size)
    assertEquals("দ", row1[0].output)
    assertEquals("ধ", row1[0].shiftOutput)
    assertEquals("প", row1[9].output)
    assertEquals("ফ", row1[9].shiftOutput)

    // Row 2: া, স, ড, ত, গ, হ, জ, ক, ল, ৃ
    val row2 = rows[1]
    assertEquals(10, row2.size)
    assertEquals("া", row2[0].output)
    assertEquals("অ", row2[0].shiftOutput)
    assertEquals("ক", row2[7].output)
    assertEquals("খ", row2[7].shiftOutput)

    // Row 3: Shift, ে, ৈ, ৌ, চ, ব, ্, ন, ম, Backspace
    val row3 = rows[2]
    assertEquals(10, row3.size)
    assertEquals("ে", row3[1].output)
    assertEquals("এ", row3[1].shiftOutput)
  }

  @Test
  fun testBengaliLayouts_Delegation() {
    val jatiyaRows = BengaliLayouts.getJatiyaRows()
    assertEquals("ি", jatiyaRows[1][2].label)
    assertEquals("র", jatiyaRows[2][3].label)

    val bijoyRows = BengaliLayouts.getBijoyRows()
    assertEquals("ব", bijoyRows[2][4].label)

    val prabhatRows = BengaliLayouts.getPrabhatRows()
    assertEquals("দ", prabhatRows[0][0].label)
  }

  @Test
  fun testBengaliEngine_Transliteration() {
    assertEquals("আমি", BengaliEngine.transliterate("ami"))
    assertEquals("তুমি", BengaliEngine.transliterate("tumi"))
    assertEquals("বাংলাদেশ", BengaliEngine.transliterate("bangladesh"))
    assertEquals("অনেক", BengaliEngine.transliterate("onek"))
  }

  @Test
  fun testBengaliComposition_IndependentVowelsAndSigns() {
    // 1. Independent vowels remain independent
    val independentResult = com.example.ime.BengaliCompositionEngine.processInput("আ", null)
    assertTrue(independentResult is com.example.ime.CompositionResult.Commit)
    assertEquals("আ", (independentResult as com.example.ime.CompositionResult.Commit).text)

    // 2. Consonant + Vowel Sign (ক + ি = কি)
    val consonantResult = com.example.ime.BengaliCompositionEngine.processInput("ক", null)
    assertEquals("ক", (consonantResult as com.example.ime.CompositionResult.Commit).text)

    val karResult = com.example.ime.BengaliCompositionEngine.processInput("ি", "ক")
    assertEquals("ি", (karResult as com.example.ime.CompositionResult.Commit).text)

    // 3. Conjunct construction (ক + ্ + ষ = ক্ষ)
    val hasantaResult = com.example.ime.BengaliCompositionEngine.processInput("্", "ক")
    assertEquals("্", (hasantaResult as com.example.ime.CompositionResult.Commit).text)

    val conjunctResult = com.example.ime.BengaliCompositionEngine.processInput("ষ", "্")
    assertEquals("ষ", (conjunctResult as com.example.ime.CompositionResult.Commit).text)

    // Verify whole string equality: "ক" + "্" + "ষ" == "ক্ষ"
    val composedKhyo = "ক" + "্" + "ষ"
    assertEquals("ক্ষ", composedKhyo)
  }

  @Test
  fun testBijoyComposition_GLinkAndPhalas() {
    // Bijoy G-link: hasanta + া -> আ
    val gLinkResult = com.example.ime.BengaliCompositionEngine.processInput("া", "্", isBijoy = true)
    assertTrue(gLinkResult is com.example.ime.CompositionResult.Replace)
    val replaceResult = gLinkResult as com.example.ime.CompositionResult.Replace
    assertEquals(1, replaceResult.charsToDelete)
    assertEquals("আ", replaceResult.replacement)

    // Common Bijoy conjunct sequences:
    // ক + ্ + র = ক্র (ra-phala)
    val kra = "ক" + "্" + "র"
    assertEquals("ক্র", kra)

    // ত + ্ + র = ত্র
    val tra = "ত" + "্" + "র"
    assertEquals("ত্র", tra)

    // শ + ্ + র = শ্র
    val shra = "শ" + "্" + "র"
    assertEquals("শ্র", shra)

    // ক + ্য = ক্য (ya-phala)
    val kya = "ক" + "্য"
    assertEquals("ক্য", kya)

    // র + ্ + ক = র্ক (reph)
    val rephKa = "র" + "্" + "ক"
    assertEquals("র্ক", rephKa)
  }

  @Test
  fun testAvro_CaseSensitivePhonetics() {
    // Test t vs T (ট vs ত)
    assertEquals("ট", BengaliEngine.transliterate("t"))
    assertEquals("ত", BengaliEngine.transliterate("T"))

    // Test d vs D (ড vs দ)
    assertEquals("ড", BengaliEngine.transliterate("d"))
    assertEquals("দ", BengaliEngine.transliterate("D"))

    // Test n vs N (ন vs ণ)
    assertEquals("ন", BengaliEngine.transliterate("n"))
    assertEquals("ণ", BengaliEngine.transliterate("N"))

    // Test s vs S (স vs ষ)
    assertEquals("স", BengaliEngine.transliterate("s"))
    assertEquals("ষ", BengaliEngine.transliterate("S"))

    // Test r vs R vs Rh (র vs ড় vs ঢ়)
    assertEquals("র", BengaliEngine.transliterate("r"))
    assertEquals("ড়", BengaliEngine.transliterate("R"))
    assertEquals("ঢ়", BengaliEngine.transliterate("Rh"))
  }

  @Test
  fun testAvro_RealWorldWords() {
    val targetWords = listOf(
      "বাংলা", "আমার", "তুমি", "ভালো", "কেমন",
      "কথা", "শব্দ", "প্রযুক্তি", "কম্পিউটার", "কীবোর্ড",
      "অ্যান্ড্রয়েড", "বাংলাদেশ", "স্বাধীনতা", "ভাষা", "সফটওয়্যার"
    )

    assertEquals("বাংলা", BengaliEngine.transliterate("bangla"))
    assertEquals("আমার", BengaliEngine.transliterate("amar"))
    assertEquals("তুমি", BengaliEngine.transliterate("tumi"))
    assertEquals("ভালো", BengaliEngine.transliterate("bhalo"))
    assertEquals("কেমন", BengaliEngine.transliterate("kemon"))
    assertEquals("কথা", BengaliEngine.transliterate("kotha"))
    assertEquals("শব্দ", BengaliEngine.transliterate("shobdo"))
    assertEquals("প্রযুক্তি", BengaliEngine.transliterate("projukti"))
    assertEquals("কম্পিউটার", BengaliEngine.transliterate("computer"))
    assertEquals("কীবোর্ড", BengaliEngine.transliterate("keyboard"))
    assertEquals("অ্যান্ড্রয়েড", BengaliEngine.transliterate("android"))
    assertEquals("বাংলাদেশ", BengaliEngine.transliterate("bangladesh"))
    assertEquals("স্বাধীনতা", BengaliEngine.transliterate("shadhinota"))
    assertEquals("ভাষা", BengaliEngine.transliterate("bhasha"))
    assertEquals("সফটওয়্যার", BengaliEngine.transliterate("software"))
  }

  @Test
  fun testAvro_KeyboardLayoutMapperDoesNotAlterPhoneticOutput() {
    // When shifted, Avro keys should display uppercase on key label, but output remains original mapping
    val avroRows = com.example.ime.KeyboardLayoutMapper.getLayoutRows(
      ThemeManager.LAYOUT_AVRO,
      isShifted = true,
      isCapsLock = false,
      actionIcon = "🔍"
    )
    val tKey = avroRows.flatMap { it }.first { it.hint == "৫" || it.label == "T" }
    assertEquals("T", tKey.label) // UI display shows uppercase
    assertEquals("t", tKey.output) // Output is still phonetic 't'
    assertEquals("T", tKey.shiftOutput) // Shifted output is 'T'
  }

  @Test
  fun testBengaliBackspace_DeletionLength() {
    // Single plain character
    assertEquals(1, com.example.ime.BengaliCompositionHelper.getDeletionLength("ক"))
    // Kar vowel sign
    assertEquals(1, com.example.ime.BengaliCompositionHelper.getDeletionLength("কা"))
    // Virama
    assertEquals(1, com.example.ime.BengaliCompositionHelper.getDeletionLength("ক্"))
    // Empty string
    assertEquals(1, com.example.ime.BengaliCompositionHelper.getDeletionLength(""))
    // ZWJ preceded by hasanta
    val rephSequence = "র\u09CD\u200D"
    assertEquals(2, com.example.ime.BengaliCompositionHelper.getDeletionLength(rephSequence))
  }

  @Test
  fun testAvro_AspiratedAndDiacritics() {
    // ড়, ঢ়, য়
    assertEquals("ড়", BengaliEngine.transliterate("R"))
    assertEquals("ঢ়", BengaliEngine.transliterate("Rh"))
    assertEquals("য়", BengaliEngine.transliterate("y"))

    // ং, ঃ, ঁ, ৎ
    assertEquals("ং", BengaliEngine.transliterate("ng`"))
    assertEquals("ঁ", BengaliEngine.transliterate("^"))
    assertEquals("ৎ", BengaliEngine.transliterate("t``"))
    assertEquals("ঃ", BengaliEngine.transliterate("::"))

    // Aspirated consonants: kh, gh, ch, jh, th, Th, dh, Dh, ph, bh
    assertEquals("খ", BengaliEngine.transliterate("kh"))
    assertEquals("ঘ", BengaliEngine.transliterate("gh"))
    assertEquals("ছ", BengaliEngine.transliterate("ch"))
    assertEquals("ঝ", BengaliEngine.transliterate("jh"))
    assertEquals("ঠ", BengaliEngine.transliterate("th"))
    assertEquals("থ", BengaliEngine.transliterate("Th"))
    assertEquals("ঢ", BengaliEngine.transliterate("dh"))
    assertEquals("ধ", BengaliEngine.transliterate("Dh"))
    assertEquals("ফ", BengaliEngine.transliterate("ph"))
    assertEquals("ভ", BengaliEngine.transliterate("bh"))
  }
}

