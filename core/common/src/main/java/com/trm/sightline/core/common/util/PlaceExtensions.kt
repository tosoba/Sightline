package com.trm.sightline.core.common.util

import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.Place

val Place.location: GeoLocation
  get() = GeoLocation(latitude = latitude, longitude = longitude)
