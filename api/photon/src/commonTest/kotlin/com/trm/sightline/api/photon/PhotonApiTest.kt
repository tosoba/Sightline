package com.trm.sightline.api.photon

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class PhotonApiTest {
  private val emptyFeatureCollection = """{"type":"FeatureCollection","features":[]}"""

  @Test
  fun `reverse encodes radius and repeats osm_tag once per element`() = runTest {
    val (api, requests) = api()

    api.reverse(
      lat = 52.237049,
      lon = 21.017532,
      radius = 1.5,
      osmTag = listOf("amenity:pharmacy", "amenity:hospital"),
    )

    val url = requests.single()
    assertEquals("/reverse", url.encodedPath)
    assertEquals("1.5", url.parameters["radius"])
    assertEquals(
      listOf("amenity:pharmacy", "amenity:hospital"),
      url.parameters.getAll("osm_tag"),
    )
  }

  @Test
  fun `reverse omits radius and osm_tag when not provided`() = runTest {
    val (api, requests) = api()

    api.reverse(lat = 52.237049, lon = 21.017532)

    val url = requests.single()
    assertEquals("/reverse", url.encodedPath)
    assertNull(url.parameters["radius"])
    assertNull(url.parameters["osm_tag"])
  }

  @Test
  fun `search encodes the query and omits the optional parameters`() = runTest {
    val (api, requests) = api()

    api.search(query = "eiffel tower", limit = 5)

    val url = requests.single()
    assertEquals("/api", url.encodedPath)
    assertEquals("eiffel tower", url.parameters["q"])
    assertEquals("5", url.parameters["limit"])
    assertNull(url.parameters["lat"])
    assertNull(url.parameters["lon"])
    assertNull(url.parameters["lang"])
  }

  private fun api(): Pair<PhotonApi, List<Url>> {
    val requests = mutableListOf<Url>()
    val engine =
      MockEngine { request ->
        requests += request.url
        respond(
          content = emptyFeatureCollection,
          status = HttpStatusCode.OK,
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
      }
    val client =
      HttpClient(engine) {
        install(ContentNegotiation) {
          json(
            Json {
              ignoreUnknownKeys = true
              explicitNulls = false
            }
          )
        }
      }
    return PhotonApi(client, baseUrl = "https://photon.test") to requests
  }
}
