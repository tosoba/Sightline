package com.trm.sightline.core.domain

interface AddressRepository {
  suspend fun getAddress(latitude: Double, longitude: Double): String?
}
