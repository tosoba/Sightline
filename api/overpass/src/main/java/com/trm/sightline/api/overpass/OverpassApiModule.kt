package com.trm.sightline.api.overpass

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OverpassApiModule {
  @Provides
  @Singleton
  fun provideOverpassApi(client: OkHttpClient): OverpassApi = OverpassApi.create(client)
}
