package com.trm.sightline.api.overpass

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OverpassApiModule {
  @Provides
  @Singleton
  fun provideOverpassApi(): OverpassApi = OverpassApi.create()
}
