package com.trm.sightline.core.data

import com.trm.sightline.api.photon.Feature
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class FeatureMapperTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `maps a photon feature to a place with a type-prefixed id`() {
    val feature =
      feature(
        """
        {
          "type": "Feature",
          "properties": { "name": "Berlin Hauptbahnhof", "osm_type": "W", "osm_id": 236582001 },
          "geometry": { "type": "Point", "coordinates": [13.369402, 52.525083] }
        }
        """,
      )

    val place = FeatureMapper.toPlace(feature, PlaceCategory.GettingAround)

    assertEquals(
      Place(
        id = "W:236582001",
        name = "Berlin Hauptbahnhof",
        latitude = 52.525083,
        longitude = 13.369402,
        category = PlaceCategory.GettingAround,
      ),
      place,
    )
  }

  @Test
  fun `trims a whitespace-padded name`() {
    val feature =
      feature(
        """
        {
          "type": "Feature",
          "properties": { "name": "  Eiffel Tower  ", "osm_type": "W", "osm_id": 50246063 },
          "geometry": { "type": "Point", "coordinates": [2.2945, 48.8584] }
        }
        """,
      )

    assertEquals(
      "Eiffel Tower",
      FeatureMapper.toPlace(feature, PlaceCategory.SightsLandmarks)?.name,
    )
  }

  @Test
  fun `returns null for a feature with a blank name`() {
    val feature =
      feature(
        """
        {
          "type": "Feature",
          "properties": { "name": "   ", "osm_type": "N", "osm_id": 1 },
          "geometry": { "type": "Point", "coordinates": [0.0, 0.0] }
        }
        """,
      )

    assertNull(FeatureMapper.toPlace(feature, PlaceCategory.Practical))
  }

  @Test
  fun `returns null for a feature without a point geometry`() {
    val feature =
      feature(
        """
        {
          "type": "Feature",
          "properties": { "name": "An area", "osm_type": "W", "osm_id": 2 },
          "geometry": { "type": "Polygon", "coordinates": [] }
        }
        """,
      )

    assertNull(FeatureMapper.toPlace(feature, PlaceCategory.NatureOutdoors))
  }

  @Test
  fun `returns null for a feature missing its osm identity`() {
    val feature =
      feature(
        """
        {
          "type": "Feature",
          "properties": { "name": "No id" },
          "geometry": { "type": "Point", "coordinates": [0.0, 0.0] }
        }
        """,
      )

    assertNull(FeatureMapper.toPlace(feature, PlaceCategory.Shopping))
  }

  private fun feature(json: String): Feature = this.json.decodeFromString(json)
}
