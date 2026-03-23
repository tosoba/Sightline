package com.trm.sightline.core.data

import com.trm.sightline.api.photon.PhotonApi
import com.trm.sightline.core.domain.AddressRepository
import javax.inject.Inject

class AddressNetworkRepository @Inject constructor(private val photonApi: PhotonApi) :
  AddressRepository {
  override suspend fun getAddress(latitude: Double, longitude: Double): String? {
    val feature =
      photonApi.reverse(lat = latitude, lon = longitude).features()?.firstOrNull() ?: return null
    val parts =
      listOfNotNull(
          feature.getStringProperty("street"),
          feature.getStringProperty("district") ?: feature.getStringProperty("locality"),
          feature.getStringProperty("city"),
          feature.getStringProperty("country"),
        )
        .filter(String::isNotBlank)
    if (parts.isEmpty()) return null
    return parts.joinToString(", ")
  }
}
