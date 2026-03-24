package com.trm.sightline.core.data

import com.trm.sightline.api.photon.PhotonApi
import com.trm.sightline.core.domain.AddressRepository
import com.trm.sightline.core.model.SearchResult
import javax.inject.Inject

class AddressNetworkRepository @Inject constructor(private val photonApi: PhotonApi) :
  AddressRepository {
  override suspend fun getAddress(latitude: Double, longitude: Double): String? {
    val feature =
      photonApi.reverse(lat = latitude, lon = longitude).features()?.firstOrNull() ?: return null
    return AddressMapper.toAddress(feature)
  }

  override suspend fun search(query: String, limit: Int): List<SearchResult> {
    val result = photonApi.search(query = query, limit = limit)
    return result.features()?.mapNotNull(AddressMapper::toSearchResult).orEmpty()
  }
}
