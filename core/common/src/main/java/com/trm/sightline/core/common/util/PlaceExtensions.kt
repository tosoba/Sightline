package com.trm.sightline.core.common.util

import android.location.Location
import com.trm.sightline.core.model.Place

val Place.location: Location
  get() =
    Location("").also {
      it.latitude = latitude
      it.longitude = longitude
    }
