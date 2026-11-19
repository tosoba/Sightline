package com.trm.sightline.core.datastore

import androidx.datastore.core.DataMigration
import com.trm.sightline.core.model.UserPreferences
import java.io.File

const val LEGACY_USER_PREFERENCES_FILE_NAME = "user_preferences.pb"

class LegacyProtobufPreferencesMigration(private val legacyFile: File) :
  DataMigration<UserPreferences> {
  override suspend fun shouldMigrate(currentData: UserPreferences): Boolean =
    legacyFile.exists() && currentData == UserPreferences()

  override suspend fun migrate(currentData: UserPreferences): UserPreferences =
    LegacyUserPreferencesReader.read(legacyFile.readBytes()) ?: currentData

  override suspend fun cleanUp() {
    legacyFile.delete()
  }
}
