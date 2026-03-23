package com.trm.sightline.api.photon

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhotonApiModule {
  @Provides @Singleton fun providePhotonApi(): PhotonApi = PhotonApi.create()
}
