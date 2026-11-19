package com.trm.sightline.core.datastore

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.trm.sightline.core.model.UserPreferences
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath

actual fun userPreferencesStorage(directoryPath: String): Storage<UserPreferences> =
  OkioStorage(
    fileSystem = FileSystem.SYSTEM,
    serializer = UserPreferencesOkioSerializer,
    producePath = { "$directoryPath/$USER_PREFERENCES_FILE_NAME".toPath() },
  )

private object UserPreferencesOkioSerializer : OkioSerializer<UserPreferences> {
  override val defaultValue: UserPreferences = UserPreferences()

  override suspend fun readFrom(source: BufferedSource): UserPreferences =
    UserPreferencesJson.decode(source.readUtf8())

  override suspend fun writeTo(t: UserPreferences, sink: BufferedSink) {
    sink.writeUtf8(UserPreferencesJson.encode(t))
  }
}
