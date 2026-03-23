package com.trm.sightline.api.photon

import kotlinx.coroutines.test.runTest
import org.junit.Test

class PhotonApiTest {
  private val photonApi = PhotonApi.create()

  @Test
  fun search() = runTest {
    println(photonApi.search(query = "Aleje Jerozolimskie", lat = 52.237049, lon = 21.017532))
  }

  @Test
  fun reverseGeocoding() = runTest { println(photonApi.reverse(lat = 52.237049, lon = 21.017532)) }
}
