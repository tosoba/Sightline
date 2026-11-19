package com.trm.sightline.core.datastore

import androidx.datastore.core.CorruptionException
import com.trm.sightline.core.model.UserPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserPreferencesJsonTest {
  private val preferences =
    UserPreferences(
      userLocationEnabled = true,
      customLocationLatitude = 52.52,
      customLocationLongitude = 13.405,
      customLocationAddress = "Berlin",
      customLocationTimestampMillis = 1_700_000_000_000,
      lastMapLatitude = 52.5,
      lastMapLongitude = 13.4,
      lastMapZoom = 12.5,
      lastMapBearing = 90.0,
      lastMapPitch = 45.0,
      searchRadiusMeters = 2000,
    )

  @Test
  fun `round trips every field`() {
    assertEquals(preferences, UserPreferencesJson.decode(UserPreferencesJson.encode(preferences)))
  }

  @Test
  fun `defaults to an empty preference set`() {
    assertEquals(UserPreferences(), UserPreferencesJson.decode("{}"))
  }

  @Test
  fun `ignores unknown keys`() {
    assertEquals(UserPreferences(), UserPreferencesJson.decode("""{"unknown":"value"}"""))
  }

  @Test
  fun `maps unparseable json to a corruption exception`() {
    assertFailsWith<CorruptionException> { UserPreferencesJson.decode("not json") }
  }
}
