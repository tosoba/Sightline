package com.trm.sightline.feature.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import kotlinx.serialization.Serializable

@Serializable
data class PlaceCategoryRoute(val category: PlaceCategory, val places: List<Place>) : NavKey

@Composable
fun PlaceCategoryScreen(
  route: PlaceCategoryRoute,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize())
}
