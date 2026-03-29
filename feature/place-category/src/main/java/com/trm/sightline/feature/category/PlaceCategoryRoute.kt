package com.trm.sightline.feature.category

import androidx.navigation3.runtime.NavKey
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import kotlinx.serialization.Serializable

@Serializable
data class PlaceCategoryRoute(val category: PlaceCategory, val places: List<Place>) : NavKey
