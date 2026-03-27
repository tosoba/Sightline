package com.trm.sightline.core.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides
  @Singleton
  fun provideCache(@ApplicationContext context: Context): Cache =
    Cache(File(context.cacheDir, "http_cache"), 10 * 1024 * 1024L)

  @Provides
  @Singleton
  fun provideOkHttpClient(cache: Cache): OkHttpClient = OkHttpClient.Builder().cache(cache).build()

  @Provides
  @Singleton
  fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
}
