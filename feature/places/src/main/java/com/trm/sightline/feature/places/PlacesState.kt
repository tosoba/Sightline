package com.trm.sightline.feature.places

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

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
