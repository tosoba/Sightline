package com.trm.sightline.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Stable
class AdaptiveSheetScaffoldState internal constructor(
  val isCompactHeight: Boolean,
  val scaffoldState: BottomSheetScaffoldState,
  val expandedProgress: State<Float>,
  val sheetOffset: State<Float>,
  val sheetNonPeekHeight: MutableFloatState,
  val contentPadding: PaddingValues,
) {
  val sheetState: SheetState
    get() = scaffoldState.bottomSheetState

  val cameraPreviewBlurred: Boolean
    get() = !isCompactHeight && expandedProgress.value != 0f
}

fun adaptiveSheetContentPadding(
  isCompactHeight: Boolean,
  sideSheetWidth: Dp,
  sheetPeekHeight: Dp,
  safeDrawingBottom: Dp,
): PaddingValues =
  if (isCompactHeight) {
    PaddingValues(bottom = safeDrawingBottom, end = sideSheetWidth)
  } else {
    PaddingValues(bottom = sheetPeekHeight)
  }

/**
 * Hosts a [BottomSheetScaffold] in portrait and a right-aligned [Surface] side sheet in compact
 * height, sharing its drag handle, inset math and expanded-progress tracking between screens.
 *
 * [sheetContentFillModifier] sizes the portrait sheet content before navigation-bar and horizontal
 * padding are applied; the compact side sheet always fills the available height.
 * [applyContentPadding] decides whether the scaffold's content insets wrap [content] or not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheetScaffold(
  isCompactHeight: Boolean,
  sheetPeekHeight: Dp,
  sheetContainerColor: Color,
  modifier: Modifier = Modifier,
  applyContentPadding: Boolean = true,
  sheetContentHorizontalPadding: Dp = 16.dp,
  sheetContentFillModifier: Modifier = Modifier.fillMaxSize(),
  sheetContent: @Composable (AdaptiveSheetScaffoldState, Modifier) -> Unit,
  content: @Composable BoxScope.(AdaptiveSheetScaffoldState, PaddingValues) -> Unit,
) {
  val scaffoldState = rememberBottomSheetScaffoldStateForScreenHeight(isCompactHeight)
  val progress = rememberBottomSheetExpandedProgress(scaffoldState.bottomSheetState)

  val safeDrawingBottom =
    WindowInsets.safeDrawing
      .only(WindowInsetsSides.Bottom)
      .asPaddingValues()
      .calculateBottomPadding()
  val contentPadding =
    adaptiveSheetContentPadding(
      isCompactHeight = isCompactHeight,
      sideSheetWidth = sideSheetWidthDp.dp,
      sheetPeekHeight = sheetPeekHeight,
      safeDrawingBottom = safeDrawingBottom,
    )

  val state =
    remember(isCompactHeight, scaffoldState, contentPadding) {
      AdaptiveSheetScaffoldState(
        isCompactHeight = isCompactHeight,
        scaffoldState = scaffoldState,
        expandedProgress = progress.expandedProgress,
        sheetOffset = progress.sheetOffset,
        sheetNonPeekHeight = progress.nonPeekHeightState,
        contentPadding = contentPadding,
      )
    }

  val density = LocalDensity.current
  val sideSheetModifier =
    Modifier.width(sideSheetWidthDp.dp)
      .fillMaxHeight()
      .systemBarsPadding()
      .navigationBarsPadding()
      .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
      .padding(horizontal = sheetContentHorizontalPadding)
  val portraitSheetModifier =
    sheetContentFillModifier
      .navigationBarsPadding()
      .padding(horizontal = sheetContentHorizontalPadding)
      .onGloballyPositioned { layoutCoordinates ->
        state.sheetNonPeekHeight.floatValue =
          layoutCoordinates.size.height.toFloat() - with(density) { sheetPeekHeight.toPx() }
      }

  Box(modifier = modifier.fillMaxSize()) {
    BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetDragHandle = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Spacer(
            modifier =
              Modifier.height(
                WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() *
                  state.expandedProgress.value
              )
          )
          BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface)
        }
      },
      sheetContainerColor = sheetContainerColor,
      sheetPeekHeight = sheetPeekHeight,
      sheetContent = {
        sheetContent(state, if (isCompactHeight) sideSheetModifier else portraitSheetModifier)
      },
    ) { innerPadding ->
      Box(
        modifier =
          if (applyContentPadding) {
            Modifier.fillMaxSize()
              .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = if (isCompactHeight) innerPadding.calculateBottomPadding() else 0.dp,
                start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
              )
          } else {
            Modifier.fillMaxSize()
          },
        contentAlignment = Alignment.Center,
      ) {
        content(state, innerPadding)

        if (isCompactHeight) {
          Surface(
            modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
            color = sheetContainerColor,
            tonalElevation = 1.dp,
          ) {
            sheetContent(state, sideSheetModifier)
          }
        }
      }
    }
  }
}
