package com.trm.sightline.core.datastore

import androidx.datastore.core.FileStorage
import androidx.datastore.core.Serializer
import androidx.datastore.core.Storage
import com.trm.sightline.core.model.UserPreferences
import java.io.File
import java.io.InputStream
import java.io.OutputStream

actual fun userPreferencesStorage(directoryPath: String): Storage<UserPreferences> =
  FileStorage(
    serializer = UserPreferencesAndroidSerializer,
    produceFile = { File(directoryPath, USER_PREFERENCES_FILE_NAME) },
  )

private object UserPreferencesAndroidSerializer : Serializer<UserPreferences> {
  override val defaultValue: UserPreferences = UserPreferences()

  override suspend fun readFrom(input: InputStream): UserPreferences =
    UserPreferencesJson.decode(input.readBytes().decodeToString())

  override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
    output.write(UserPreferencesJson.encode(t).encodeToByteArray())
  }
}
