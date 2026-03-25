package com.trm.sightline.api.photon

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhotonApiModule {
  @Provides
  @Singleton
  fun providePhotonApi(client: OkHttpClient): PhotonApi = PhotonApi.create(client)
}
