package com.trm.sightline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.trm.sightline.feature.category.PlaceCategoryRoute
import com.trm.sightline.feature.category.PlaceCategoryScreen
import com.trm.sightline.feature.map.MapPreview
import com.trm.sightline.ui.theme.SightlineTheme
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalSharedTransitionApi::class)
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

        SharedTransitionLayout {
          NavDisplay(
            backStack = backStack,
            entryProvider =
              entryProvider {
                entry<MainRoute> {
                  MainScreen(
                    isCompactHeight = isCompactHeight,
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    onCategoryClick = { category, places ->
                      backStack.add(PlaceCategoryRoute(category, places))
                    },
                  )
                }
                entry<PlaceCategoryRoute> { route ->
                  PlaceCategoryScreen(
                    route = route,
                    isCompactHeight = isCompactHeight,
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                  ) {
                    MapPreview(
                      places = route.places,
                      padding = it,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }
              },
          )
        }
      }
    }
  }
}
