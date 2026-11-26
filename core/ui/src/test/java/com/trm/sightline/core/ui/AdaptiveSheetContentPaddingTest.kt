package com.trm.sightline.core.ui

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveSheetContentPaddingTest {
  @Test
  fun `compact height reserves the side sheet width and safe drawing bottom inset`() {
    val padding =
      adaptiveSheetContentPadding(
        isCompactHeight = true,
        sideSheetWidth = 400.dp,
        sheetPeekHeight = 0.dp,
        safeDrawingBottom = 24.dp,
      )

    assertEquals(24.dp, padding.calculateBottomPadding())
    assertEquals(400.dp, padding.calculateEndPadding(LayoutDirection.Ltr))
    assertEquals(0.dp, padding.calculateTopPadding())
    assertEquals(0.dp, padding.calculateStartPadding(LayoutDirection.Ltr))
  }

  @Test
  fun `portrait reserves the sheet peek height as bottom padding`() {
    val padding =
      adaptiveSheetContentPadding(
        isCompactHeight = false,
        sideSheetWidth = 400.dp,
        sheetPeekHeight = 234.dp,
        safeDrawingBottom = 24.dp,
      )

    assertEquals(234.dp, padding.calculateBottomPadding())
    assertEquals(0.dp, padding.calculateEndPadding(LayoutDirection.Ltr))
  }
}
