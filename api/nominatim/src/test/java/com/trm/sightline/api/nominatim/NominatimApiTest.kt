package com.trm.sightline.api.nominatim

import kotlinx.coroutines.test.runTest
import org.junit.Test

class NominatimApiTest {
  private val nominatimApi = NominatimApi.create()

  @Test
  fun reverseGeocoding() = runTest { println(nominatimApi.reverse(lat = 52.2297, lon = 21.0122)) }
}
