package com.trm.sightline.api.photon

import kotlinx.coroutines.test.runTest
import org.junit.Test

class PhotonApiTest {
  private val photonApi = PhotonApi.create()

  @Test fun searchBerlin() = runTest { println(photonApi.search(query = "Berlin")) }

  @Test
  fun reverseGeocoding() = runTest { println(photonApi.reverse(lat = 52.5200, lon = 13.4050)) }
}
