package com.trm.sightline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.trm.sightline.feature.category.PlaceCategoryRoute
import com.trm.sightline.feature.category.PlaceCategoryScreen
import com.trm.sightline.ui.theme.SightlineTheme
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

        NavDisplay(
          backStack = backStack,
          entryProvider =
            entryProvider {
              entry<MainRoute> {
                MainScreen(
                  isCompactHeight = isCompactHeight,
                  onCategoryClick = { category, places ->
                    backStack.add(PlaceCategoryRoute(category, places))
                  },
                )
              }
              entry<PlaceCategoryRoute> { route -> PlaceCategoryScreen(route) }
            },
        )
      }
    }
  }
}
