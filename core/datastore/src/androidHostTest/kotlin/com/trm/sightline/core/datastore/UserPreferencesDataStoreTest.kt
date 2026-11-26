package com.trm.sightline.core.datastore

import com.trm.sightline.core.model.UserPreferences
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class UserPreferencesDataStoreTest {
  private val directory =
    File.createTempFile("user-preferences", "").apply {
      delete()
      mkdirs()
    }
  private val storageFile = File(directory, USER_PREFERENCES_FILE_NAME)
  private val legacyFile = File(directory, LEGACY_USER_PREFERENCES_FILE_NAME)

  @AfterTest
  fun cleanUp() {
    directory.deleteRecursively()
  }

  @Test
  fun `persists preferences as json in the storage file`() = runTest {
    val dataStore = createUserPreferencesDataStore(userPreferencesStorage(directory.absolutePath))

    val preferences =
      UserPreferences(userLocationEnabled = true, searchRadiusMeters = 2000, lastMapZoom = 12.5)
    dataStore.updateData { preferences }

    assertEquals(preferences, dataStore.data.first())
    assertTrue(storageFile.readText().contains("\"searchRadiusMeters\":2000"))
  }

  @Test
  fun `migrates the legacy protobuf file on first read`() = runTest {
    legacyFile.writeBytes(
      hex(
        "08 01" + // user_location_enabled = true
          " 22 06 42 65 72 6C 69 6E" + // custom_location_address = "Berlin"
          " 28 E8 07" + // custom_location_timestamp = 1000
          " 58 DC 0B", // search_radius = 1500
      ),
    )

    val dataStore =
      createUserPreferencesDataStore(
        storage = userPreferencesStorage(directory.absolutePath),
        migrations = listOf(LegacyProtobufPreferencesMigration(legacyFile)),
      )

    assertEquals(
      UserPreferences(
        userLocationEnabled = true,
        customLocationAddress = "Berlin",
        customLocationTimestampMillis = 1000,
        searchRadiusMeters = 1500,
      ),
      dataStore.data.first(),
    )
    assertFalse(legacyFile.exists())
    assertTrue(storageFile.exists())
  }

  @Test
  fun `keeps already persisted preferences over the legacy file`() = runTest {
    storageFile.parentFile?.mkdirs()
    storageFile.writeText(UserPreferencesJson.encode(UserPreferences(searchRadiusMeters = 500)))
    legacyFile.writeBytes(hex("08 01 58 DC 0B"))

    val dataStore =
      createUserPreferencesDataStore(
        storage = userPreferencesStorage(directory.absolutePath),
        migrations = listOf(LegacyProtobufPreferencesMigration(legacyFile)),
      )

    assertEquals(UserPreferences(searchRadiusMeters = 500), dataStore.data.first())
    assertTrue(legacyFile.exists())
  }

  private fun hex(value: String): ByteArray =
    value.split(" ").filter(String::isNotBlank).map { it.toInt(radix = 16).toByte() }.toByteArray()
}
