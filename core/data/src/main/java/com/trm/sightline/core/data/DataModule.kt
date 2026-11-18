package com.trm.sightline.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.trm.sightline.core.datastore.UserPreferences
import com.trm.sightline.core.datastore.UserPreferencesSerializer
import com.trm.sightline.core.domain.UserPreferencesRepository
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
  fun provideUserPreferencesDataStore(context: Context): DataStore<UserPreferences> =
    DataStoreFactory.create(
      serializer = UserPreferencesSerializer,
      produceFile = { File(context.filesDir, "datastore/user_preferences.pb") },
    )

  @Provides
  fun provideUserPreferencesRepository(
    dataStore: DataStore<UserPreferences>
  ): UserPreferencesRepository = UserPreferencesLocalRepository(dataStore)
}
