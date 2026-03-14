package com.trm.sightline.feature.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesContent(state: PlacesState, modifier: Modifier = Modifier) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    SearchBar(
      expanded = false,
      onExpandedChange = {},
      modifier = Modifier.fillMaxWidth(),
      windowInsets = WindowInsets(),
      inputField = {
        SearchBarDefaults.InputField(
          query = state.location,
          onQueryChange = { state.location = it },
          onSearch = {},
          expanded = false,
          enabled = !state.userLocation,
          onExpandedChange = {},
          placeholder = { Text("Current location") },
          leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            FilledTonalIconToggleButton(
              checked = state.userLocation,
              onCheckedChange = { state.userLocation = it },
            ) {
              Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My location")
            }
          },
        )
      },
    ) {}

    Spacer(modifier = Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      state.categories.forEach { category ->
        PlaceCategoryToggleButton(
          label = category,
          icon =
            when (category) {
              "Attractions" -> Icons.Default.Place
              "Food" -> Icons.Default.Restaurant
              "Accommodation" -> Icons.Default.Hotel
              "Stores" -> Icons.Default.Storefront
              else -> throw IllegalStateException()
            },
          isSelected = category in state.selectedCategories,
          onClick = { state.toggleCategory(category) },
        )
      }
    }
  }
}
