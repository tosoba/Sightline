package com.trm.sightline.api.overpass

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OverpassApiModule {
  @Provides
  @Singleton
  fun provideOverpassApi(
    client: OkHttpClient,
    loggingInterceptor: HttpLoggingInterceptor,
  ): OverpassApi =
    OverpassApi.create(
      if (BuildConfig.DEBUG) {
        client.newBuilder().addInterceptor(loggingInterceptor).build()
      } else {
        client
      }
    )
}
