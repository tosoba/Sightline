package com.trm.sightline

import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.trm.sightline.core.ar.util.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ar.util.sideSheetWidthDp
import com.trm.sightline.core.model.Marker
import com.trm.sightline.feature.places.PlacesContent
import com.trm.sightline.feature.places.rememberPlacesState
import com.trm.sightline.ui.theme.SightlineTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalMaterial3WindowSizeClassApi::class,
)
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SightlineTheme {
        val scope = rememberCoroutineScope()

        val pagerState = rememberPagerState(pageCount = MainPage.entries::size)
        val selectedPage = MainPage.entries[pagerState.currentPage]
        var bottomControlsVisible by remember { mutableStateOf(true) }
        LaunchedEffect(pagerState.currentPage) { bottomControlsVisible = true }

        val isCompactHeight =
          calculateWindowSizeClass(this).heightSizeClass == WindowHeightSizeClass.Compact
        val sheetState =
          key(isCompactHeight) {
            rememberStandardBottomSheetState(
              initialValue =
                if (isCompactHeight) SheetValue.Hidden else SheetValue.PartiallyExpanded,
              skipHiddenState = !isCompactHeight,
            )
          }
        val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
        LaunchedEffect(isCompactHeight) { if (isCompactHeight) sheetState.hide() }

        val location = remember {
          Location(null).apply {
            latitude = 52.237049
            longitude = 21.017532
          }
        }
        val markers = remember {
          List(10) { index ->
            Marker(
              name = "Marker ${index + 1}",
              latitude = 52.237049 + ((index + 1) * 0.001),
              longitude = 21.017532 + ((index + 1) * 0.001),
            )
          }
        }

        val density = LocalDensity.current
        val sheetOffset = runCatching { sheetState.requireOffset() }.getOrDefault(0f)
        val sheetPeekHeight =
          if (isCompactHeight) {
            0.dp
          } else {
            collapsedBottomSheetContentHeightDp.dp +
              WindowInsets.navigationBars.getBottom(LocalDensity.current).dp
          }
        var sheetHeightPx by remember { mutableFloatStateOf(0f) }
        val transitionProgress =
          remember(sheetOffset, sheetHeightPx) {
            if (sheetHeightPx > 0f) (sheetOffset / sheetHeightPx).coerceIn(0f, 1f) else 0f
          }
        val transitionThreshold = .5f
        val thresholdProgress =
          remember(transitionProgress) {
            ((transitionProgress - transitionThreshold) / (1f - transitionThreshold)).coerceIn(
              0f,
              1f,
            )
          }
        val expandedProgress =
          remember(thresholdProgress) { 1f - thresholdProgress }.coerceIn(0f, 1f)

        val placesSheetState = rememberPlacesState()
        val placesSheetContent =
          @Composable {
            PlacesContent(
              state = placesSheetState,
              modifier =
                if (isCompactHeight) {
                  Modifier.width(sideSheetWidthDp.dp)
                    .fillMaxHeight()
                    .systemBarsPadding()
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
                    .padding(horizontal = 16.dp)
                } else {
                  Modifier.fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                      sheetHeightPx =
                        layoutCoordinates.size.height.toFloat() -
                          with(density) { sheetPeekHeight.toPx() }
                    }
                },
            )
          }

        val backStack = rememberNavBackStack(MainRoute)
        NavDisplay(
          backStack = backStack,
          entryProvider =
            entryProvider {
              entry<MainRoute> {
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
                              expandedProgress
                          )
                      )
                      BottomSheetDefaults.DragHandle()
                    }
                  },
                  sheetContainerColor = BottomSheetDefaults.ContainerColor.copy(alpha = .5f),
                  sheetPeekHeight = sheetPeekHeight,
                  sheetContent = { placesSheetContent() },
                ) { innerPadding ->
                  Box(
                    modifier =
                      Modifier.fillMaxSize()
                        .padding(
                          top = innerPadding.calculateTopPadding(),
                          bottom =
                            if (isCompactHeight) innerPadding.calculateBottomPadding() else 0.dp,
                          start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                          end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                        ),
                    contentAlignment = Alignment.Center,
                  ) {
                    MainPager(
                      pagerState = pagerState,
                      location = location,
                      markers = markers,
                      isCompactHeight = isCompactHeight,
                      cameraPreviewBlurred = !isCompactHeight && expandedProgress != 0f,
                      cameraPreviewOverlayVisible = bottomControlsVisible,
                      onCameraPreviewTouch = { bottomControlsVisible = !bottomControlsVisible },
                    )

                    MainPagerToolbar(
                      visible =
                        bottomControlsVisible &&
                          (isCompactHeight || sheetState.targetValue != SheetValue.Expanded),
                      isCompactHeight = isCompactHeight,
                      selectedPage = selectedPage,
                      onPageSelected = { page ->
                        scope.launch { pagerState.animateScrollToPage(page.ordinal) }
                      },
                    )

                    if (isCompactHeight) {
                      Surface(
                        modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                        color = BottomSheetDefaults.ContainerColor.copy(alpha = .5f),
                        tonalElevation = 1.dp,
                      ) {
                        placesSheetContent()
                      }
                    }
                  }
                }
              }
            },
        )
      }
    }
  }
}

@Serializable private data object MainRoute : NavKey
