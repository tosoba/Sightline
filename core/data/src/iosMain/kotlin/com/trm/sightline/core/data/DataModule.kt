package com.trm.sightline.core.data

import androidx.datastore.core.DataStore
import com.trm.sightline.core.datastore.createUserPreferencesDataStore
import com.trm.sightline.core.datastore.userPreferencesStorage
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.UserPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object DataModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideUserPreferencesDataStore(
    directory: UserPreferencesDirectory,
  ): DataStore<UserPreferences> =
    createUserPreferencesDataStore(storage = userPreferencesStorage(directory.path))

  @Provides
  fun provideUserPreferencesRepository(
    dataStore: DataStore<UserPreferences>,
  ): UserPreferencesRepository = UserPreferencesLocalRepository(dataStore)
}
