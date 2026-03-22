package com.trm.sightline

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trm.sightline.composable.MainPager
import com.trm.sightline.composable.MainPagerToolbar
import com.trm.sightline.core.ar.util.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ar.util.sideSheetWidthDp
import com.trm.sightline.core.common.PermissionStatus
import com.trm.sightline.core.common.rememberPermissionState
import com.trm.sightline.core.common.util.CheckLocationSettingsResult
import com.trm.sightline.core.common.util.checkLocationSettings
import com.trm.sightline.core.common.util.startAppSettingsActivity
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.feature.places.PlacesContent
import com.trm.sightline.feature.places.PlacesLayout
import kotlinx.coroutines.launch

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalSharedTransitionApi::class,
)
@Composable
fun SharedTransitionScope.MainScreen(
  isCompactHeight: Boolean,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onCategoryClick: (PlaceCategory, List<Place>) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val pagerState = rememberPagerState(pageCount = MainPage.entries::size)
  val containerAlpha by remember {
    derivedStateOf {
      val progress =
        (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
      0.5f + (progress * 0.4f)
    }
  }
  val contentAlpha by remember {
    derivedStateOf {
      val progress =
        (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
      0.5f + (progress * 0.5f)
    }
  }
  val selectedPage = MainPage.entries[pagerState.currentPage]
  var toolbarsVisible by remember { mutableStateOf(true) }
  LaunchedEffect(pagerState.currentPage) { toolbarsVisible = true }

  val viewModel = hiltViewModel<MainViewModel>()

  val locationSettingsLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result
      ->
      if (result.resultCode == Activity.RESULT_OK) viewModel.setUserLocationEnabled(true)
    }
  var showLocationDisabledDialog by remember { mutableStateOf(false) }

  suspend fun checkLocationSettings() {
    when (val result = context.checkLocationSettings()) {
      CheckLocationSettingsResult.Enabled -> {
        viewModel.setUserLocationEnabled(true)
      }
      is CheckLocationSettingsResult.DisabledResolvable -> {
        locationSettingsLauncher.launch(result.intentSenderRequest)
      }
      CheckLocationSettingsResult.DisabledNonResolvable -> {
        showLocationDisabledDialog = true
      }
    }
  }

  if (showLocationDisabledDialog) {
    AlertDialog(
      onDismissRequest = { showLocationDisabledDialog = false },
      title = { Text("Location is disabled") },
      text = { Text("Device location is turned off. Enable it in Settings to use this feature.") },
      confirmButton = {
        TextButton(
          onClick = {
            showLocationDisabledDialog = false
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
          }
        ) {
          Text("Open Settings")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLocationDisabledDialog = false }) { Text("Cancel") }
      },
    )
  }

  val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
  var locationPermissionFlowInProgress by remember { mutableStateOf(false) }
  LaunchedEffect(locationPermissionState.status) {
    when (locationPermissionState.status) {
      PermissionStatus.Granted -> {
        if (locationPermissionFlowInProgress) checkLocationSettings()
        locationPermissionFlowInProgress = false
      }
      PermissionStatus.Denied,
      PermissionStatus.PermanentlyDenied -> {
        locationPermissionFlowInProgress = false
      }
      PermissionStatus.Unknown -> {}
    }
  }

  var showLocationPermissionSettingsDialog by remember { mutableStateOf(false) }
  if (showLocationPermissionSettingsDialog) {
    AlertDialog(
      onDismissRequest = { showLocationPermissionSettingsDialog = false },
      title = { Text("Location permission required") },
      text = { Text("Location access was permanently denied. Open Settings to grant it.") },
      confirmButton = {
        TextButton(
          onClick = {
            showLocationPermissionSettingsDialog = false
            context.startAppSettingsActivity()
          }
        ) {
          Text("Open Settings")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLocationPermissionSettingsDialog = false }) { Text("Cancel") }
      },
    )
  }

  val sheetState =
    key(isCompactHeight) {
      rememberStandardBottomSheetState(
        initialValue = if (isCompactHeight) SheetValue.Hidden else SheetValue.PartiallyExpanded,
        skipHiddenState = !isCompactHeight,
      )
    }
  val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
  LaunchedEffect(isCompactHeight) { if (isCompactHeight) sheetState.hide() }

  val focusManager = LocalFocusManager.current
  LaunchedEffect(sheetState.targetValue) {
    if (sheetState.targetValue == SheetValue.PartiallyExpanded) {
      focusManager.clearFocus()
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
      ((transitionProgress - transitionThreshold) / (1f - transitionThreshold)).coerceIn(0f, 1f)
    }
  val expandedProgress = remember(thresholdProgress) { 1f - thresholdProgress }.coerceIn(0f, 1f)

  val placesSheetContent =
    @Composable {
      PlacesContent(
        places = viewModel.places,
        customLocationAddress = viewModel.customLocationAddress.collectAsStateWithLifecycle().value,
        userLocationEnabled =
          viewModel.userLocationEnabled.collectAsStateWithLifecycle().value &&
            locationPermissionState.isGranted,
        placeCategoriesEnabled = viewModel.povLocation?.location != null,
        layout =
          if (isCompactHeight || sheetState.targetValue == SheetValue.Expanded) PlacesLayout.Grid
          else PlacesLayout.Row,
        alpha = contentAlpha,
        animatedVisibilityScope = animatedVisibilityScope,
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
                  layoutCoordinates.size.height.toFloat() - with(density) { sheetPeekHeight.toPx() }
              }
          },
        onSearchFocusChange = { isFocused ->
          if (isFocused && !isCompactHeight) {
            scope.launch { sheetState.expand() }
          }
        },
        onToggleUserLocationEnabled = { enabled ->
          if (enabled) {
            when (locationPermissionState.status) {
              PermissionStatus.Granted -> {
                scope.launch { checkLocationSettings() }
              }
              PermissionStatus.PermanentlyDenied -> {
                showLocationPermissionSettingsDialog = true
              }
              else -> {
                locationPermissionFlowInProgress = true
                locationPermissionState.launchRequest()
              }
            }
          } else {
            locationPermissionFlowInProgress = false
            viewModel.setUserLocationEnabled(false)
          }
        },
        onLocationChange = viewModel::setCustomLocationAddress,
        onTogglePlaceCategory = viewModel::onTogglePlaceCategory,
        onCategoryClick = onCategoryClick,
      )
    }

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
              WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() * expandedProgress
            )
        )
        BottomSheetDefaults.DragHandle()
      }
    },
    sheetContainerColor = BottomSheetDefaults.ContainerColor.copy(alpha = containerAlpha),
    sheetPeekHeight = sheetPeekHeight,
    sheetContent = { placesSheetContent() },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            top = innerPadding.calculateTopPadding(),
            bottom = if (isCompactHeight) innerPadding.calculateBottomPadding() else 0.dp,
            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
          ),
      contentAlignment = Alignment.Center,
    ) {
      MainPager(
        pagerState = pagerState,
        location = viewModel.povLocation?.location,
        places = viewModel.allPlaces,
        isCompactHeight = isCompactHeight,
        cameraPreviewBlurred = !isCompactHeight && expandedProgress != 0f,
        cameraPreviewOverlayVisible = toolbarsVisible,
        onCameraPreviewTouch = { toolbarsVisible = !toolbarsVisible },
      )

      MainPagerToolbar(
        visible =
          toolbarsVisible && (isCompactHeight || sheetState.targetValue != SheetValue.Expanded),
        isCompactHeight = isCompactHeight,
        selectedPage = selectedPage,
        onPageSelected = { page -> scope.launch { pagerState.animateScrollToPage(page.ordinal) } },
      )

      if (isCompactHeight) {
        Surface(
          modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
          color = BottomSheetDefaults.ContainerColor.copy(alpha = containerAlpha),
          tonalElevation = 1.dp,
        ) {
          placesSheetContent()
        }
      }
    }
  }
}
