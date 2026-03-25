package com.trm.sightline.core.domain

import com.trm.sightline.core.model.CustomLocation

interface AddressRepository {
  suspend fun getAddress(latitude: Double, longitude: Double): String?

  suspend fun search(query: String, limit: Int): List<CustomLocation>
}
