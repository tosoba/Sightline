package com.trm.sightline.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import com.trm.sightline.core.datastore.LegacyProtobufPreferencesMigration
import com.trm.sightline.core.datastore.LEGACY_USER_PREFERENCES_FILE_NAME
import com.trm.sightline.core.datastore.USER_PREFERENCES_DIRECTORY
import com.trm.sightline.core.datastore.createUserPreferencesDataStore
import com.trm.sightline.core.datastore.userPreferencesStorage
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.UserPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File

@ContributesTo(AppScope::class)
@BindingContainer
object DataModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideUserPreferencesDataStore(context: Context): DataStore<UserPreferences> {
    val directory = File(context.filesDir, USER_PREFERENCES_DIRECTORY)
    return createUserPreferencesDataStore(
      storage = userPreferencesStorage(directory.absolutePath),
      migrations =
        listOf(
          LegacyProtobufPreferencesMigration(
            File(directory, LEGACY_USER_PREFERENCES_FILE_NAME)
          )
        ),
    )
  }

  @Provides
  fun provideUserPreferencesRepository(
    dataStore: DataStore<UserPreferences>
  ): UserPreferencesRepository = UserPreferencesLocalRepository(dataStore)
}
