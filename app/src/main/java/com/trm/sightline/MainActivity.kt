package com.trm.sightline

import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.ar.model.Marker
import com.trm.sightline.core.ar.model.SimpleARMarker
import com.trm.sightline.feature.camera.CameraPermissionState
import com.trm.sightline.feature.camera.CameraPreview
import com.trm.sightline.feature.camera.rememberCameraPermissionState
import com.trm.sightline.feature.map.MapPreview
import com.trm.sightline.ui.theme.SightlineTheme
import kotlinx.coroutines.launch

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3WindowSizeClassApi::class,
  ExperimentalMaterial3ExpressiveApi::class,
)
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SightlineTheme {
        val cameraPermissionState = rememberCameraPermissionState()
        val pagerState = rememberPagerState { MainPage.entries.size }
        val scope = rememberCoroutineScope()
        val selectedPage = MainPage.entries[pagerState.currentPage]

        LaunchedEffect(Unit) {
          if (!cameraPermissionState.isGranted) {
            cameraPermissionState.launchRequest()
          }
        }

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

        val placesSheetState = rememberPlacesState()
        val sheetContent =
          @Composable {
            PlacesSheetContent(
              state = placesSheetState,
              modifier =
                if (isCompactHeight) {
                  Modifier.width(320.dp)
                    .fillMaxHeight()
                    .systemBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                } else {
                  Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp)
                },
            )
          }

        BottomSheetScaffold(
          scaffoldState = scaffoldState,
          sheetPeekHeight =
            if (isCompactHeight) {
              0.dp
            } else {
              160.dp + 26.dp + WindowInsets.navigationBars.getBottom(LocalDensity.current).dp
            },
          sheetContent = { sheetContent() },
        ) { innerPadding ->
          Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
          ) {
            HorizontalPager(
              state = pagerState,
              modifier = Modifier.fillMaxSize(),
              beyondViewportPageCount = 1,
              userScrollEnabled = false,
            ) { page ->
              when (MainPage.entries[page]) {
                MainPage.Camera -> {
                  CameraContent(
                    cameraPermissionState = cameraPermissionState,
                    previewEnabled = pagerState.currentPage == MainPage.Camera.ordinal,
                  )
                }
                MainPage.Map -> {
                  MapPreview(modifier = Modifier.fillMaxSize())
                }
              }
            }

            HorizontalFloatingToolbar(
              expanded = true,
              modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            ) {
              MainPage.entries.forEach { page ->
                MainPagerToolbarItem(
                  isSelected = selectedPage == page,
                  onClick = { scope.launch { pagerState.animateScrollToPage(page.ordinal) } },
                  icon = page.icon,
                  label = page.label,
                )
              }
            }

            if (isCompactHeight) {
              Surface(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
              ) {
                sheetContent()
              }
            }
          }
        }
      }
    }
  }
}



enum class MainPage(val icon: ImageVector, val label: String) {
  Camera(Icons.Default.PhotoCamera, "Camera"),
  Map(Icons.Default.Map, "Map"),
}

@Composable
fun MainPagerToolbarItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    selected = isSelected,
    onClick = onClick,
    shape = CircleShape,
    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    contentColor =
      if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
      else MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))

      if (isSelected) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}

@Composable
fun CameraContent(cameraPermissionState: CameraPermissionState, previewEnabled: Boolean) {
  AnimatedContent(cameraPermissionState.isGranted) {
    if (it) {
      CameraPreview(
        location =
          remember {
            Location(null).apply {
              latitude = 52.237049
              longitude = 21.017532
            }
          },
        markers =
          remember {
            List(10) { index ->
              SimpleARMarker(
                Marker(
                  "Marker ${index + 1}",
                  Location(null).apply {
                    latitude = 52.237049 + ((index + 1) * 0.001)
                    longitude = 21.017532 + ((index + 1) * 0.001)
                  },
                )
              )
            }
          },
        enabled = previewEnabled,
      )
    } else {
      Button(onClick = cameraPermissionState::launchRequest) {
        Text(text = "Grant camera permission")
      }
    }
  }
}

@Composable
fun PlacesSheetContent(state: PlacesState, modifier: Modifier = Modifier) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = state.location,
        onValueChange = { state.location = it },
        modifier = Modifier.weight(1f),
        placeholder = { Text("Current location") },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
        shape = CircleShape,
      )

      Spacer(modifier = Modifier.width(12.dp))

      IconToggleButton(
        checked = state.userLocation,
        onCheckedChange = { state.userLocation = it },
        modifier = Modifier.fillMaxHeight().aspectRatio(1f),
      ) {
        Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My location")
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      PlaceCategoryToggleButton(
        label = "All",
        icon = Icons.Default.Apps,
        isSelected = state.isAllSelected,
        onClick = state::toggleAll,
      )

      state.categories.forEach { category ->
        PlaceCategoryToggleButton(
          label = category,
          icon =
            when (category) {
              "Attractions" -> Icons.Default.Place
              "Accommodation" -> Icons.Default.Hotel
              "Stores" -> Icons.Default.Storefront
              else -> Icons.Default.Category
            },
          isSelected = category in state.selectedCategories,
          onClick = { state.toggleCategory(category) },
        )
      }
    }
  }
}

@Stable
class PlacesState(
  location: String = "",
  userLocation: Boolean = false,
  selectedCategories: List<String> = emptyList(),
) {
  var location by mutableStateOf(location)
  var userLocation by mutableStateOf(userLocation)
  val selectedCategories = mutableStateSetOf(*selectedCategories.toTypedArray())

  val categories = listOf("Attractions", "Accommodation", "Stores")
  val isAllSelected: Boolean
    get() = selectedCategories.size == categories.size

  fun toggleCategory(category: String) {
    if (!selectedCategories.remove(category)) selectedCategories.add(category)
  }

  fun toggleAll() {
    if (isAllSelected) selectedCategories.clear() else selectedCategories.addAll(categories)
  }

  companion object {
    val Saver: Saver<PlacesState, *> =
      listSaver(
        save = { listOf(it.location, it.userLocation, it.selectedCategories.toList()) },
        restore = {
          @Suppress("UNCHECKED_CAST")
          PlacesState(
            location = it[0] as String,
            userLocation = it[1] as Boolean,
            selectedCategories = it[2] as List<String>,
          )
        },
      )
  }
}

@Composable
fun rememberPlacesState(): PlacesState =
  rememberSaveable(saver = PlacesState.Saver, init = ::PlacesState)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RowScope.PlaceCategoryToggleButton(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  ToggleButton(
    checked = isSelected,
    onCheckedChange = { onClick() },
    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(vertical = 8.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
      )
    }
  }
}
