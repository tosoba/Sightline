package com.trm.sightline.core.common.util

import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberBottomSheetScaffoldStateForScreenHeight(
  isCompactHeight: Boolean
): BottomSheetScaffoldState {
  val sheetState =
    key(isCompactHeight) {
      rememberStandardBottomSheetState(
        initialValue = if (isCompactHeight) SheetValue.Hidden else SheetValue.PartiallyExpanded,
        skipHiddenState = !isCompactHeight,
      )
    }
  val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
  LaunchedEffect(isCompactHeight) { if (isCompactHeight) sheetState.hide() }
  return scaffoldState
}
