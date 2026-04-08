package com.trm.sightline.core.common.util

import android.location.Location
import com.trm.sightline.core.model.Place

val Place.location: Location
  get() =
    Location("").also {
      it.latitude = latitude
      it.longitude = longitude
    }

val Place.tourismOrLeisure: String?
  get() = (tags["tourism"] ?: tags["leisure"])?.replace('_', ' ')?.replaceFirstChar(Char::uppercase)

val Place.formattedAddress: String?
  get() =
    listOfNotNull(
        listOfNotNull(tags["addr:street"], tags["addr:housenumber"])
          .filter(String::isNotBlank)
          .joinToString(" ")
          .takeIf(String::isNotBlank),
        tags["addr:city"].takeUnless(String?::isNullOrBlank)?.trim(),
        listOfNotNull(tags["addr:state"], tags["addr:postcode"])
          .filter(String::isNotBlank)
          .joinToString(" ")
          .trim()
          .takeIf(String::isNotBlank),
      )
      .joinToString(", ")
      .trim()
      .takeIf(String::isNotBlank)
