package com.trm.sightline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.trm.sightline.core.ui.theme.SightlineTheme
import com.trm.sightline.feature.category.PlaceCategoryRoute
import com.trm.sightline.feature.category.PlaceCategoryScreen
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SightlineTheme {
        val isCompactHeight =
          calculateWindowSizeClass(this).heightSizeClass == WindowHeightSizeClass.Compact
        val backStack = rememberNavBackStack(MainRoute)

        val viewModel = hiltViewModel<MainViewModel>()
        val userLocationEnabled by viewModel.userLocationEnabled.collectAsStateWithLifecycle()
        val customLocationAddress by viewModel.customLocationAddress.collectAsStateWithLifecycle()
        val userLocationAddress by viewModel.userLocationAddress.collectAsStateWithLifecycle()
        val customLocationSearchResults by
          viewModel.customLocationSearchResults.collectAsStateWithLifecycle()
        val lastMapPosition by viewModel.lastMapPosition.collectAsStateWithLifecycle()
        val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
        val searchRadius by viewModel.searchRadius.collectAsStateWithLifecycle()
        val currentLocation =
          remember(userLocationEnabled, viewModel.userLocation, viewModel.customLocation) {
            if (userLocationEnabled && viewModel.userLocation != null) viewModel.userLocation
            else viewModel.customLocation
          }

        SharedTransitionLayout {
          NavDisplay(
            backStack = backStack,
            predictivePopTransitionSpec = {
              ContentTransform(
                fadeIn(spring(dampingRatio = 1.0f, stiffness = 1600.0f)),
                fadeOut(spring(dampingRatio = 1.0f, stiffness = 1600.0f)),
              )
            },
            entryProvider =
              entryProvider {
                entry<MainRoute> {
                  MainScreen(
                    isCompactHeight = isCompactHeight,
                    places = viewModel.places,
                    allPlaces = viewModel.allPlaces,
                    currentLocation = currentLocation,
                    userLocation = viewModel.userLocation,
                    userLocationAddress = userLocationAddress,
                    customLocation = viewModel.customLocation,
                    customLocationAddress = customLocationAddress,
                    customLocationSearchResults = customLocationSearchResults,
                    isUserLocationEnabled = userLocationEnabled,
                    lastMapPosition = lastMapPosition,
                    errorMessage = errorMessage,
                    searchRadius = searchRadius,
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    onUserLocationEnabledChange = viewModel::setUserLocationEnabled,
                    onCustomLocationAddressChange = viewModel::setCustomLocationAddress,
                    onCustomLocationSearchResultClick =
                      viewModel::onCustomLocationSearchResultClick,
                    onTogglePlaceCategory = viewModel::onTogglePlaceCategory,
                    onCategoryClick = { category, places ->
                      backStack.add(PlaceCategoryRoute(category, places))
                    },
                    onMapPositionChanged = viewModel::saveMapPosition,
                    onSearchRadiusChange = viewModel::setSearchRadius,
                  )
                }
                entry<PlaceCategoryRoute> { route ->
                  PlaceCategoryScreen(
                    route = route,
                    location = currentLocation,
                    isCompactHeight = isCompactHeight,
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                  )
                }
              },
          )
        }
      }
    }
  }
}
