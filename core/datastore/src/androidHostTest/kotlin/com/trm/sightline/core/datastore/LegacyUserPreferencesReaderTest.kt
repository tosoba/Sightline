package com.trm.sightline.core.datastore

import com.trm.sightline.core.model.UserPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LegacyUserPreferencesReaderTest {
  @Test
  fun `reads the scalar fields of the legacy message`() {
    val bytes =
      hex(
        "08 01" + // user_location_enabled = true
          " 11 00 00 00 00 00 00 4A 40" + // custom_location_lat = 52.0
          " 19 00 00 00 00 00 00 2B 40" + // custom_location_lng = 13.5
          " 22 06 42 65 72 6C 69 6E" + // custom_location_address = "Berlin"
          " 28 E8 07" + // custom_location_timestamp = 1000
          " 31 00 00 00 00 00 00 29 40" + // last_map_lat = 12.5
          " 58 DC 0B" // search_radius = 1500
      )

    assertEquals(
      UserPreferences(
        userLocationEnabled = true,
        customLocationLatitude = 52.0,
        customLocationLongitude = 13.5,
        customLocationAddress = "Berlin",
        customLocationTimestampMillis = 1000,
        lastMapLatitude = 12.5,
        searchRadiusMeters = 1500,
      ),
      LegacyUserPreferencesReader.read(bytes),
    )
  }

  @Test
  fun `skips fields it does not know`() {
    val bytes = hex("7A 03 61 62 63 08 01")

    assertEquals(UserPreferences(userLocationEnabled = true), LegacyUserPreferencesReader.read(bytes))
  }

  @Test
  fun `returns null for truncated data`() {
    assertNull(LegacyUserPreferencesReader.read(hex("22 06 42")))
  }

  private fun hex(value: String): ByteArray =
    value.split(" ").filter(String::isNotBlank).map { it.toInt(radix = 16).toByte() }.toByteArray()
}
