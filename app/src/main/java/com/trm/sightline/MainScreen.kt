package com.trm.sightline

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trm.sightline.composable.MainPager
import com.trm.sightline.composable.MainPagerToolbar
import com.trm.sightline.composable.MainScreenErrorMessage
import com.trm.sightline.core.ar.math.RoundedRectF
import com.trm.sightline.core.ar.util.dpToPx
import com.trm.sightline.core.ar.util.getScreenSize
import com.trm.sightline.core.ar.util.sideSheetRectF
import com.trm.sightline.core.common.PermissionStatus
import com.trm.sightline.core.common.messageResource
import com.trm.sightline.core.common.util.CheckLocationSettingsResult
import com.trm.sightline.core.common.util.checkLocationSettings
import com.trm.sightline.core.common.util.startAppSettingsActivity
import com.trm.sightline.core.domain.places.LocationAddressState
import com.trm.sightline.core.domain.places.PlacesUiState
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.model.PlaceSearchRadius
import com.trm.sightline.core.ui.AdaptiveSheetScaffold
import com.trm.sightline.core.ui.collapsedBottomSheetContentHeightDp
import com.trm.sightline.core.ui.rememberPermissionState
import com.trm.sightline.feature.places.PlacesContent
import com.trm.sightline.feature.places.PlacesLayout
import kotlinx.coroutines.launch
import com.trm.sightline.core.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.MainScreen(
  state: PlacesUiState,
  isCompactHeight: Boolean,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onUserLocationEnabledChange: (Boolean) -> Unit,
  onCustomLocationAddressChange: (String) -> Unit,
  onCustomLocationSearchResultClick: (CustomLocation) -> Unit,
  onCancelCustomLocationSearch: () -> Unit,
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

  val userLocationEnabled = state.userLocationEnabled && locationPermissionState.isGranted
  val locationAddress =
    if (userLocationEnabled) {
      state.userLocationAddress
    } else {
      LocationAddressState.Loaded(state.customLocationAddress)
    }
  val errorMessage = state.error?.messageResource()
  val focusManager = LocalFocusManager.current

  val sheetPeekHeight =
    if (isCompactHeight) {
      0.dp
    } else {
      collapsedBottomSheetContentHeightDp.dp +
        WindowInsets.navigationBars.getBottom(LocalDensity.current).dp
    }

  val sheetContainerColor =
    when (selectedPage) {
      MainPage.Map -> BottomSheetDefaults.ContainerColor.copy(alpha = .9f)
      MainPage.Camera if cameraPermissionState.isGranted -> Color.Transparent
      else -> BottomSheetDefaults.ContainerColor
    }

  Box(modifier = Modifier.fillMaxSize()) {
    AdaptiveSheetScaffold(
      isCompactHeight = isCompactHeight,
      sheetPeekHeight = sheetPeekHeight,
      sheetContainerColor = sheetContainerColor,
      sheetContent = { sheet, sheetModifier ->
        PlacesContent(
          places = state.places,
          userLocationEnabled = userLocationEnabled,
          activeLocation = state.activeLocation,
          locationAddress = locationAddress,
          customLocationSearchResults = state.customLocationSearchResults,
          layout =
            if (isCompactHeight || sheet.cameraPreviewBlurred) PlacesLayout.Grid
            else PlacesLayout.Row,
          alpha =
            when (selectedPage) {
              MainPage.Camera -> .75f
              MainPage.Map -> 1f
            },
          animatedVisibilityScope = animatedVisibilityScope,
          modifier = sheetModifier,
          onSearchFocusChange = { isFocused ->
            if (isFocused && !isCompactHeight) {
              scope.launch { sheet.sheetState.expand() }
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
          onCancelCustomLocationSearch = onCancelCustomLocationSearch,
          onTogglePlaceCategory = onTogglePlaceCategory,
          onCategoryClick = onCategoryClick,
        )
      },
    ) { sheet ->
      LaunchedEffect(sheet.sheetState.targetValue) {
        if (sheet.sheetState.targetValue == SheetValue.PartiallyExpanded) {
          focusManager.clearFocus()
        }
      }

      MainPager(
        pagerState = pagerState,
        location = state.activeLocation,
        places = state.allPlaces,
        lastMapPosition = state.lastMapPosition,
        isCompactHeight = isCompactHeight,
        cameraPreviewBlurred = sheet.cameraPreviewBlurred,
        blurredRectFs =
          listOf(
            with(LocalContext.current) {
              if (isCompactHeight) {
                val sideSheetRect = sideSheetRectF
                RoundedRectF(
                  left = sideSheetRect.left,
                  top = sideSheetRect.top,
                  right = sideSheetRect.right,
                  bottom = sideSheetRect.bottom,
                  cornerRadius = 0f,
                )
              } else {
                val screenSize = getScreenSize()
                RoundedRectF(
                  left = 0f,
                  top = sheet.sheetOffset.value,
                  right = screenSize.width.toFloat(),
                  bottom = screenSize.height.toFloat(),
                  cornerRadius = dpToPx(32f),
                )
              }
            }
          ),
        cameraPreviewOverlayVisible = toolbarsVisible,
        cameraPermissionGranted = cameraPermissionState.isGranted,
        onCameraPermissionGrantClick = {
          if (cameraPermissionState.status == PermissionStatus.PermanentlyDenied) {
            showCameraPermissionSettingsDialog = true
          } else {
            cameraPermissionState.launchRequest()
          }
        },
        contentPadding = sheet.contentPadding,
        onCameraPreviewTouch = { toolbarsVisible = !toolbarsVisible },
        onMapPositionChanged = onMapPositionChanged,
      )

      MainPagerToolbar(
        visible = toolbarsVisible && (isCompactHeight || !sheet.cameraPreviewBlurred),
        isCompactHeight = isCompactHeight,
        selectedPage = selectedPage,
        searchRadius = state.searchRadius,
        onPageSelected = { page ->
          scope.launch { pagerState.animateScrollToPage(page.ordinal) }
        },
        onSearchRadiusChange = onSearchRadiusChange,
      )
    }

    MainScreenErrorMessage(
      message = errorMessage?.let { stringResource(it) }.orEmpty(),
      visible = errorMessage != null,
      modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).systemBarsPadding(),
    )
  }
}
