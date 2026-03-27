package com.trm.sightline.api.photon

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhotonApiModule {
  @Provides
  @Singleton
  fun providePhotonApi(
    client: OkHttpClient,
    loggingInterceptor: HttpLoggingInterceptor,
  ): PhotonApi =
    PhotonApi.create(
      if (BuildConfig.DEBUG) {
        client.newBuilder().addInterceptor(loggingInterceptor).build()
      } else {
        client
      }
    )
}
