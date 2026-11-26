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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.trm.sightline.core.ui.theme.SightlineTheme
import com.trm.sightline.feature.category.PlaceCategoryRoute
import com.trm.sightline.feature.category.PlaceCategoryScreen
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val factory = (application as SightlineApp).appGraph.metroViewModelFactory
    setContent {
      CompositionLocalProvider(LocalMetroViewModelFactory provides factory) {
        SightlineTheme {
          val isCompactHeight =
            calculateWindowSizeClass(this).heightSizeClass == WindowHeightSizeClass.Compact
          val backStack = rememberNavBackStack(MainRoute)

          val viewModel = metroViewModel<MainViewModel>()
          val state by viewModel.state.collectAsStateWithLifecycle()

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
                      state = state,
                      isCompactHeight = isCompactHeight,
                      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                      onUserLocationEnabledChange = viewModel::setUserLocationEnabled,
                      onCustomLocationAddressChange = viewModel::setCustomLocationAddress,
                      onCustomLocationSearchResultClick =
                        viewModel::onCustomLocationSearchResultClick,
                      onCancelCustomLocationSearch = viewModel::cancelCustomLocationSearch,
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
                      location = state.activeLocation,
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
}
