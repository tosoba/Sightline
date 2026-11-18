package com.trm.sightline.core.common.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlin.time.Duration.Companion.seconds
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@ContributesTo(AppScope::class)
@BindingContainer
object NetworkModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideCache(context: Context): Cache =
    Cache(File(context.cacheDir, "http_cache"), 10 * 1024 * 1024L)

  @Provides
  @SingleIn(AppScope::class)
  fun provideOkHttpClient(cache: Cache): OkHttpClient =
    OkHttpClient.Builder().readTimeout(40.seconds).writeTimeout(30.seconds).cache(cache).build()

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
}
