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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trm.sightline.composable.MainPager
import com.trm.sightline.composable.MainPagerToolbar
import com.trm.sightline.composable.MainScreenErrorMessage
import com.trm.sightline.core.ar.util.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ar.util.sideSheetWidthDp
import com.trm.sightline.core.common.PermissionStatus
import com.trm.sightline.core.common.R as commonR
import com.trm.sightline.core.common.rememberPermissionState
import com.trm.sightline.core.common.util.CheckLocationSettingsResult
import com.trm.sightline.core.common.util.checkLocationSettings
import com.trm.sightline.core.common.util.rememberBottomSheetScaffoldStateForScreenHeight
import com.trm.sightline.core.common.util.startAppSettingsActivity
import com.trm.sightline.core.model.LoadingState
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
  val pageChangeProgress by remember {
    derivedStateOf {
      (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
    }
  }
  val containerAlpha = 0.5f + (pageChangeProgress * 0.4f)
  val contentAlpha = 0.5f + (pageChangeProgress * 0.5f)
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

  val placesContentAlpha by remember {
    derivedStateOf {
      if (selectedPage == MainPage.Camera && !cameraPermissionState.isGranted) 1f else contentAlpha
    }
  }
  val placesContainerAlpha by remember {
    derivedStateOf {
      if (selectedPage == MainPage.Camera && !cameraPermissionState.isGranted) 1f
      else containerAlpha
    }
  }

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

  val userLocationEnabled =
    viewModel.userLocationEnabled.collectAsStateWithLifecycle().value &&
      locationPermissionState.isGranted

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
  var sheetHeightPx by remember { mutableFloatStateOf(0f) }
  val transitionThreshold = .5f
  val sheetOffset by remember {
    derivedStateOf { runCatching { sheetState.requireOffset() }.getOrDefault(0f) }
  }
  val transitionProgress by remember {
    derivedStateOf {
      if (sheetHeightPx > 0f) (sheetOffset / sheetHeightPx).coerceIn(0f, 1f) else 0f
    }
  }
  val thresholdProgress by remember {
    derivedStateOf {
      ((transitionProgress - transitionThreshold) / (1f - transitionThreshold)).coerceIn(0f, 1f)
    }
  }
  val expandedProgress by remember { derivedStateOf { (1f - thresholdProgress).coerceIn(0f, 1f) } }

  val placesSheetContent =
    @Composable {
      val density = LocalDensity.current
      val customLocationAddress by viewModel.customLocationAddress.collectAsStateWithLifecycle()
      val gpsLocationAddress by viewModel.userLocationAddress.collectAsStateWithLifecycle()
      val customLocationSearchResults by
        viewModel.customLocationSearchResults.collectAsStateWithLifecycle()

      PlacesContent(
        places = viewModel.places,
        locationAddress =
          if (userLocationEnabled) gpsLocationAddress
          else LoadingState.Loaded(customLocationAddress),
        userLocationEnabled = userLocationEnabled,
        placeCategoriesEnabled =
          if (userLocationEnabled) viewModel.userLocation != null
          else viewModel.customLocation != null,
        customLocationSearchResults = customLocationSearchResults,
        layout =
          if (isCompactHeight || sheetState.targetValue == SheetValue.Expanded) PlacesLayout.Grid
          else PlacesLayout.Row,
        alpha = placesContentAlpha,
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
        onCustomLocationAddressChange = viewModel::setCustomLocationAddress,
        onCustomLocationSearchResultClick = viewModel::onCustomLocationSearchResultClick,
        onTogglePlaceCategory = viewModel::onTogglePlaceCategory,
        onCategoryClick = onCategoryClick,
      )
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
      sheetContainerColor = BottomSheetDefaults.ContainerColor.copy(alpha = placesContainerAlpha),
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
        val lastMapPosition by viewModel.lastMapPosition.collectAsStateWithLifecycle()
        val cameraPreviewBlurred by remember {
          derivedStateOf { !isCompactHeight && expandedProgress != 0f }
        }

        MainPager(
          pagerState = pagerState,
          location = if (userLocationEnabled) viewModel.userLocation else viewModel.customLocation,
          places = viewModel.allPlaces,
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
          onMapPositionChanged = viewModel::saveMapPosition,
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
            color = BottomSheetDefaults.ContainerColor.copy(alpha = placesContainerAlpha),
            tonalElevation = 1.dp,
          ) {
            placesSheetContent()
          }
        }
      }
    }

    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    MainScreenErrorMessage(
      message = errorMessage?.let { stringResource(it) }.orEmpty(),
      visible = errorMessage != null,
      modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).systemBarsPadding(),
    )
  }
}
