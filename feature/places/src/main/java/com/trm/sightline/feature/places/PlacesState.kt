package com.trm.sightline.feature.places

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class PlacesState(location: String = "", userLocation: Boolean = false) {
  internal var location by mutableStateOf(location)
  internal var userLocation by mutableStateOf(userLocation)

  companion object {
    val Saver: Saver<PlacesState, *> =
      listSaver(
        save = { listOf(it.location, it.userLocation) },
        restore = { PlacesState(location = it[0] as String, userLocation = it[1] as Boolean) },
      )
  }
}

@Composable
fun rememberPlacesState(): PlacesState =
  rememberSaveable(saver = PlacesState.Saver, init = ::PlacesState)
