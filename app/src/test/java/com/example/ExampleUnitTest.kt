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
}

