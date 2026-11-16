package com.trm.sightline.api.photon

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotonApiTest {
  private val emptyFeatureCollection = """{"type":"FeatureCollection","features":[]}"""

  @Test
  fun `reverse encodes radius and repeats osm_tag once per element`() = runTest {
    val server = MockWebServer()
    server.enqueue(MockResponse().setBody(emptyFeatureCollection))
    server.start()

    try {
      val api = PhotonApi.create(baseUrl = server.url("/").toString())

      api.reverse(
        lat = 52.237049,
        lon = 21.017532,
        radius = 1.5,
        osmTag = listOf("amenity:pharmacy", "amenity:hospital"),
      )

      val url = server.takeRequest().requestUrl!!

      assertEquals("/reverse", url.encodedPath)
      assertEquals("1.5", url.queryParameter("radius"))
      assertEquals(listOf("amenity:pharmacy", "amenity:hospital"), url.queryParameterValues("osm_tag"))
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun `reverse omits radius and osm_tag when not provided`() = runTest {
    val server = MockWebServer()
    server.enqueue(MockResponse().setBody(emptyFeatureCollection))
    server.start()

    try {
      val api = PhotonApi.create(baseUrl = server.url("/").toString())

      api.reverse(lat = 52.237049, lon = 21.017532)

      val url = server.takeRequest().requestUrl!!

      assertEquals("/reverse", url.encodedPath)
      assertNull(url.queryParameter("radius"))
      assertNull(url.queryParameter("osm_tag"))
    } finally {
      server.shutdown()
    }
  }
}
