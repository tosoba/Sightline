package com.trm.sightline.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.trm.sightline.core.datastore.UserPreferences
import com.trm.sightline.core.datastore.UserPreferencesSerializer
import com.trm.sightline.core.domain.AddressRepository
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
  @Binds abstract fun bindPlacesRepository(repository: PlacesNetworkRepository): PlacesRepository

  @Binds abstract fun bindAddressRepository(repository: AddressNetworkRepository): AddressRepository

  companion object {
    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
      @ApplicationContext context: Context
    ): DataStore<UserPreferences> =
      DataStoreFactory.create(
        serializer = UserPreferencesSerializer,
        produceFile = { File(context.filesDir, "datastore/user_preferences.pb") },
      )

    @Provides
    fun provideUserPreferencesRepository(
      dataStore: DataStore<UserPreferences>
    ): UserPreferencesRepository = UserPreferencesLocalRepository(dataStore)
  }
}
