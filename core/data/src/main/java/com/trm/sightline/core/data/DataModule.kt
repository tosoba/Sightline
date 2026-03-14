package com.trm.sightline.core.data

import com.trm.sightline.core.domain.PlacesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
  @Binds fun bindPlacesRepository(repository: PlacesNetworkRepository): PlacesRepository
}
