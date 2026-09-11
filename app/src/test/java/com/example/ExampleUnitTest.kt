package com.example

import com.example.ime.BengaliLayouts
import com.example.ime.BijoyLayoutMap
import com.example.ime.JatiyaLayoutMap
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
  fun testBijoyLayout_Row3Key4_FixedDuplicate() {
    val rows = BijoyLayoutMap.getRows()
    val row3 = rows[2]
    // Row 3 keys: [Shift, ৲, ো, ে, ব, ...]
    val key5InRow = row3[4] // Index 4 in row3 is Key 4 among character keys (Z, X, C, V)
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
  fun testBengaliLayouts_Delegation() {
    val jatiyaRows = BengaliLayouts.getJatiyaRows()
    assertEquals("ি", jatiyaRows[1][2].label)
    assertEquals("র", jatiyaRows[2][3].label)

    val bijoyRows = BengaliLayouts.getBijoyRows()
    assertEquals("ব", bijoyRows[2][4].label)
  }
}

