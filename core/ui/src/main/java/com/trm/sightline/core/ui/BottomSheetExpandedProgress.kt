package com.trm.sightline.core.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember

data class BottomSheetExpandedProgressState(
  val nonPeekHeightState: MutableFloatState,
  val expandedProgress: State<Float>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberBottomSheetExpandedProgress(
  sheetState: SheetState,
  transitionThreshold: Float = 0.5f,
): BottomSheetExpandedProgressState {
  val nonPeekHeightState = remember { mutableFloatStateOf(0f) }

  val sheetOffset = remember {
    derivedStateOf { runCatching { sheetState.requireOffset() }.getOrDefault(0f) }
  }
  val transitionProgress = remember {
    derivedStateOf {
      val value = nonPeekHeightState.floatValue
      if (value > 0f) (sheetOffset.value / value).coerceIn(0f, 1f) else 0f
    }
  }
  val thresholdProgress = remember {
    derivedStateOf {
      ((transitionProgress.value - transitionThreshold) / (1f - transitionThreshold)).coerceIn(
        0f,
        1f,
      )
    }
  }
  val expandedProgress = remember {
    derivedStateOf { (1f - thresholdProgress.value).coerceIn(0f, 1f) }
  }

  return remember(nonPeekHeightState, expandedProgress) {
    BottomSheetExpandedProgressState(nonPeekHeightState, expandedProgress)
  }
}
