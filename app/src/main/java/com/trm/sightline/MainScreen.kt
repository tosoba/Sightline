package com.trm.sightline

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trm.sightline.composable.MainPager
import com.trm.sightline.composable.MainPagerToolbar
import com.trm.sightline.composable.MainScreenErrorMessage
import com.trm.sightline.core.ar.util.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ar.util.sideSheetWidthDp
import com.trm.sightline.core.common.PermissionStatus
import com.trm.sightline.core.common.rememberPermissionState
import com.trm.sightline.core.common.util.CheckLocationSettingsResult
import com.trm.sightline.core.common.util.checkLocationSettings
import com.trm.sightline.core.common.util.rememberBottomSheetScaffoldStateForScreenHeight
import com.trm.sightline.core.common.util.startAppSettingsActivity
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.model.PlaceSearchRadius
import com.trm.sightline.core.ui.rememberBottomSheetExpandedProgress
import com.trm.sightline.feature.places.PlacesContent
import com.trm.sightline.feature.places.PlacesLayout
import kotlinx.coroutines.launch
import com.trm.sightline.core.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.MainScreen(
  isCompactHeight: Boolean,
  places: Map<PlaceCategory, LoadingState<List<Place>>>,
  allPlaces: List<Place>,
  currentLocation: Location?,
  userLocation: Location?,
  userLocationAddress: LoadingState<String>,
  customLocation: Location?,
  customLocationAddress: String,
  customLocationSearchResults: LoadingState<List<CustomLocation>>,
  isUserLocationEnabled: Boolean,
  lastMapPosition: MapCameraPosition?,
  errorMessage: Int?,
  searchRadius: PlaceSearchRadius,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onUserLocationEnabledChange: (Boolean) -> Unit,
  onCustomLocationAddressChange: (String) -> Unit,
  onCustomLocationSearchResultClick: (CustomLocation) -> Unit,
  onTogglePlaceCategory: (PlaceCategory) -> Unit,
  onCategoryClick: (PlaceCategory, List<Place>) -> Unit,
  onMapPositionChanged: (MapCameraPosition) -> Unit,
  onSearchRadiusChange: (PlaceSearchRadius) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val pagerState = rememberPagerState(pageCount = MainPage.entries::size)
  val selectedPage = MainPage.entries[pagerState.currentPage]
  var toolbarsVisible by remember { mutableStateOf(true) }
  LaunchedEffect(pagerState.currentPage) { toolbarsVisible = true }

  val locationSettingsLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result
      ->
      if (result.resultCode == Activity.RESULT_OK) onUserLocationEnabledChange(true)
    }
  var showLocationDisabledDialog by remember { mutableStateOf(false) }

  suspend fun checkLocationSettings() {
    when (val result = context.checkLocationSettings()) {
      CheckLocationSettingsResult.Enabled -> {
        onUserLocationEnabledChange(true)
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
      title = { Text(stringResource(R.string.location_disabled_title)) },
      text = { Text(stringResource(R.string.location_disabled_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            showLocationDisabledDialog = false
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
          }
        ) {
          Text(stringResource(commonR.string.open_settings))
        }
      },
      dismissButton = {
        TextButton(onClick = { showLocationDisabledDialog = false }) {
          Text(stringResource(commonR.string.cancel))
        }
      },
    )
  }

  val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

  LaunchedEffect(Unit) {
    if (cameraPermissionState.status == PermissionStatus.Unknown) {
      cameraPermissionState.launchRequest()
    }
  }

  var showCameraPermissionSettingsDialog by remember { mutableStateOf(false) }
  if (showCameraPermissionSettingsDialog) {
    AlertDialog(
      onDismissRequest = { showCameraPermissionSettingsDialog = false },
      title = { Text(stringResource(R.string.camera_permission_required_title)) },
      text = { Text(stringResource(R.string.camera_permission_permanently_denied_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            showCameraPermissionSettingsDialog = false
            context.startAppSettingsActivity()
          }
        ) {
          Text(stringResource(commonR.string.open_settings))
        }
      },
      dismissButton = {
        TextButton(onClick = { showCameraPermissionSettingsDialog = false }) {
          Text(stringResource(commonR.string.cancel))
        }
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
      title = { Text(stringResource(R.string.location_permission_required_title)) },
      text = { Text(stringResource(R.string.location_permission_permanently_denied_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            showLocationPermissionSettingsDialog = false
            context.startAppSettingsActivity()
          }
        ) {
          Text(stringResource(commonR.string.open_settings))
        }
      },
      dismissButton = {
        TextButton(onClick = { showLocationPermissionSettingsDialog = false }) {
          Text(stringResource(commonR.string.cancel))
        }
      },
    )
  }

  val userLocationEnabled = isUserLocationEnabled && locationPermissionState.isGranted

  val scaffoldState = rememberBottomSheetScaffoldStateForScreenHeight(isCompactHeight)
  val sheetState = scaffoldState.bottomSheetState

  val focusManager = LocalFocusManager.current
  LaunchedEffect(sheetState.targetValue) {
    if (sheetState.targetValue == SheetValue.PartiallyExpanded) {
      focusManager.clearFocus()
    }
  }

  val sheetPeekHeight =
    if (isCompactHeight) {
      0.dp
    } else {
      collapsedBottomSheetContentHeightDp.dp +
        WindowInsets.navigationBars.getBottom(LocalDensity.current).dp
    }
  val (sheetNonPeekHeightState, expandedProgressState) =
    rememberBottomSheetExpandedProgress(sheetState)
  var sheetNonPeekHeight by sheetNonPeekHeightState
  val expandedProgress by expandedProgressState

  val placesSheetContent =
    @Composable {
      val density = LocalDensity.current
      PlacesContent(
        places = places,
        locationAddress =
          if (userLocationEnabled) userLocationAddress
          else LoadingState.Loaded(customLocationAddress),
        userLocationEnabled = userLocationEnabled,
        placeCategoriesEnabled =
          if (userLocationEnabled) userLocation != null else customLocation != null,
        customLocationSearchResults = customLocationSearchResults,
        layout =
          if (isCompactHeight || sheetState.targetValue == SheetValue.Expanded) PlacesLayout.Grid
          else PlacesLayout.Row,
        alpha =
          when (selectedPage) {
            MainPage.Camera -> .75f
            MainPage.Map -> 1f
          },
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
                sheetNonPeekHeight =
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
            onUserLocationEnabledChange(false)
          }
        },
        onCustomLocationAddressChange = onCustomLocationAddressChange,
        onCustomLocationSearchResultClick = onCustomLocationSearchResultClick,
        onTogglePlaceCategory = onTogglePlaceCategory,
        onCategoryClick = onCategoryClick,
      )
    }

  val sheetContainerColor =
    when (selectedPage) {
      MainPage.Map -> BottomSheetDefaults.ContainerColor.copy(alpha = .9f)
      MainPage.Camera if cameraPermissionState.isGranted -> Color.Transparent
      else -> BottomSheetDefaults.ContainerColor
    }
  Box(modifier = Modifier.fillMaxSize()) {
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
      sheetContainerColor = sheetContainerColor,
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
        val cameraPreviewBlurred by remember {
          derivedStateOf { !isCompactHeight && expandedProgress != 0f }
        }

        MainPager(
          pagerState = pagerState,
          location = currentLocation,
          places = allPlaces,
          lastMapPosition = lastMapPosition,
          isCompactHeight = isCompactHeight,
          cameraPreviewBlurred = cameraPreviewBlurred,
          cameraPreviewOverlayVisible = toolbarsVisible,
          cameraPermissionGranted = cameraPermissionState.isGranted,
          onCameraPermissionGrantClick = {
            if (cameraPermissionState.status == PermissionStatus.PermanentlyDenied) {
              showCameraPermissionSettingsDialog = true
            } else {
              cameraPermissionState.launchRequest()
            }
          },
          contentPadding =
            PaddingValues(
              bottom =
                if (isCompactHeight) {
                  WindowInsets.safeDrawing
                    .only(WindowInsetsSides.Bottom)
                    .asPaddingValues()
                    .calculateBottomPadding()
                } else {
                  sheetPeekHeight
                },
              end = if (isCompactHeight) sideSheetWidthDp.dp else 0.dp,
            ),
          onCameraPreviewTouch = { toolbarsVisible = !toolbarsVisible },
          onMapPositionChanged = onMapPositionChanged,
        )

        MainPagerToolbar(
          visible =
            toolbarsVisible && (isCompactHeight || sheetState.targetValue != SheetValue.Expanded),
          isCompactHeight = isCompactHeight,
          selectedPage = selectedPage,
          searchRadius = searchRadius,
          onPageSelected = { page ->
            scope.launch { pagerState.animateScrollToPage(page.ordinal) }
          },
          onSearchRadiusChange = onSearchRadiusChange,
        )

        if (isCompactHeight) {
          Surface(
            modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
            color = sheetContainerColor,
            tonalElevation = 1.dp,
          ) {
            placesSheetContent()
          }
        }
      }
    }

    MainScreenErrorMessage(
      message = errorMessage?.let { stringResource(it) }.orEmpty(),
      visible = errorMessage != null,
      modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).systemBarsPadding(),
    )
  }
}
