package com.trm.sightline.shared

import com.trm.sightline.core.common.location.LocationProvider
import com.trm.sightline.core.data.UserPreferencesDirectory
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph(AppScope::class)
internal interface IosAppGraph {
  val placesRepository: PlacesRepository

  val locationProvider: LocationProvider

  val userPreferencesRepository: UserPreferencesRepository

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides directory: UserPreferencesDirectory): IosAppGraph
  }
}
