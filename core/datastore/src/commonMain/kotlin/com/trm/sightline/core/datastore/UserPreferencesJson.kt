package com.trm.sightline.core.datastore

import androidx.datastore.core.CorruptionException
import com.trm.sightline.core.model.UserPreferences
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal object UserPreferencesJson {
  private val json =
    Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
    }

  fun encode(preferences: UserPreferences): String = json.encodeToString(preferences)

  fun decode(text: String): UserPreferences =
    try {
      json.decodeFromString<UserPreferences>(text)
    } catch (exception: SerializationException) {
      throw CorruptionException("Cannot read user preferences.", exception)
    }
}
